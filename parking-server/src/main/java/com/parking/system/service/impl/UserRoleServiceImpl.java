package com.parking.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parking.common.BusinessException;
import com.parking.common.IdentityType;
import com.parking.common.user.UserVisibility;
import com.parking.common.user.UserZoneMapping;
import com.parking.system.dto.UserRolePermDTO;
import com.parking.system.dto.UserRoleUpdateDTO;
import com.parking.system.domain.ParkingZone;
import com.parking.system.domain.User;
import com.parking.system.domain.UserRole;
import com.parking.system.domain.UserRolePerm;
import com.parking.system.mapper.ParkingZoneMapper;
import com.parking.system.mapper.UserMapper;
import com.parking.system.mapper.UserRoleMapper;
import com.parking.system.mapper.UserRolePermMapper;
import com.parking.system.service.CampusRuleService;
import com.parking.system.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 小程序用户端角色（RBAC）实现：角色 -> 可见停车区域。
 * <p>角色可见区域按 roleCode 做 5 分钟本地缓存，避免每次请求都查库。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleMapper userRoleMapper;
    private final UserRolePermMapper userRolePermMapper;
    private final UserMapper userMapper;
    private final ParkingZoneMapper parkingZoneMapper;
    private final CampusRuleService campusRuleService;

    /** 角色可见区域缓存：roleCode -> { zones, expireAt }，TTL 5 分钟 */
    private final ConcurrentHashMap<String, CachedZones> cache = new ConcurrentHashMap<>();
    private static final long TTL_MS = 5L * 60 * 1000;

    @Override
    public List<Map<String, Object>> listRoles(String keyword) {
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(UserRole::getRoleCode, keyword).or().like(UserRole::getRoleName, keyword));
        }
        wrapper.orderByAsc(UserRole::getIdentityType);
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, String> allZoneNames = zoneNameMap();
        for (UserRole r : userRoleMapper.selectList(wrapper)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", String.valueOf(r.getId()));
            m.put("roleCode", r.getRoleCode());
            m.put("roleName", r.getRoleName());
            m.put("identityType", r.getIdentityType());
            m.put("identityLabel", IdentityType.label(r.getIdentityType()));
            m.put("description", r.getDescription());
            m.put("builtin", r.getBuiltin());
            List<String> zones = new ArrayList<>(zonesOf(r.getRoleCode()));
            m.put("zones", zones);
            m.put("zoneNames", zones.stream().map(z -> allZoneNames.getOrDefault(z, z)).collect(Collectors.toList()));
            Long count = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getRoleCode, r.getRoleCode()));
            m.put("userCount", count == null ? 0 : count);
            result.add(m);
        }
        return result;
    }

    @Override
    public UserRole updateRole(Long id, UserRoleUpdateDTO dto) {
        UserRole role = mustExist(id);
        if (dto == null || !StringUtils.hasText(dto.getRoleName())) {
            throw new BusinessException("角色名称不能为空");
        }
        // roleCode 与 identityType 不可变更：账号以编码绑定角色，身份类型决定默认映射
        role.setRoleName(dto.getRoleName().trim());
        role.setDescription(dto.getDescription());
        role.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.updateById(role);
        return role;
    }

    @Override
    public List<String> getRolePerms(Long roleId) {
        return new ArrayList<>(zonesOf(mustExist(roleId).getRoleCode()));
    }

    @Override
    @Transactional
    public void assignPerms(Long roleId, UserRolePermDTO dto) {
        UserRole role = mustExist(roleId);
        // 全量覆盖：先删后插
        userRolePermMapper.delete(new LambdaQueryWrapper<UserRolePerm>().eq(UserRolePerm::getRoleId, roleId));
        if (dto != null && dto.getZoneCodes() != null) {
            LocalDateTime now = LocalDateTime.now();
            Set<String> added = new HashSet<>();
            // 合法区域以「停车区域管理」中的配置为准
            Set<String> validZones = zoneNameMap().keySet();
            for (String code : dto.getZoneCodes()) {
                if (!StringUtils.hasText(code)) continue;
                String zone = code.trim();
                if (!validZones.contains(zone)) continue;
                if (!added.add(zone)) continue;
                UserRolePerm perm = new UserRolePerm();
                perm.setRoleId(roleId);
                perm.setPermCode(zone);
                perm.setCreatedAt(now);
                userRolePermMapper.insert(perm);
            }
        }
        invalidate(role.getRoleCode());
    }

    @Override
    public Map<String, String> roleNames() {
        Map<String, String> m = new LinkedHashMap<>();
        for (UserRole r : userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().orderByAsc(UserRole::getIdentityType))) {
            m.put(r.getRoleCode(), r.getRoleName());
        }
        return m;
    }

    @Override
    public Map<String, String> zoneOptions() {
        return new LinkedHashMap<>(zoneNameMap());
    }

    /** 区域 code -> 名称（含停用区域，用于历史授权数据的展示与校验） */
    private Map<String, String> zoneNameMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (ParkingZone zone : parkingZoneMapper.selectList(new LambdaQueryWrapper<ParkingZone>()
                .orderByAsc(ParkingZone::getSortOrder)
                .orderByAsc(ParkingZone::getId))) {
            map.put(zone.getZoneCode(), zone.getZoneName());
        }
        return map;
    }

    @Override
    public String resolveRoleCodeByIdentity(Integer identityType) {
        if (identityType != null) {
            UserRole role = userRoleMapper.selectOne(
                    new LambdaQueryWrapper<UserRole>().eq(UserRole::getIdentityType, identityType));
            if (role != null) return role.getRoleCode();
        }
        return UserZoneMapping.DEFAULT_USER_ROLE;
    }

    @Override
    public String roleCodeOf(User user) {
        if (user == null) return UserZoneMapping.DEFAULT_USER_ROLE;
        // 未认证用户按访客处理（与计费/预约口径一致），已认证用户使用其绑定角色
        if (campusRuleService.effectiveIdentityType(user) == IdentityType.VISITOR) {
            return resolveRoleCodeByIdentity(IdentityType.VISITOR);
        }
        if (StringUtils.hasText(user.getRoleCode())) return user.getRoleCode();
        return resolveRoleCodeByIdentity(user.getIdentityType());
    }

    @Override
    public UserVisibility visibilityOf(Long userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            return new UserVisibility(zonesOf(resolveRoleCodeByIdentity(IdentityType.VISITOR)), IdentityType.VISITOR);
        }
        String roleCode = roleCodeOf(user);
        Set<String> zones = zonesOf(roleCode);
        // 放行判定身份：已认证用真实身份，未认证按访客（由 CampusRuleService 统一口径）
        Integer identity = user.getIdentityType();
        if (!isVerified(user)) identity = IdentityType.VISITOR;
        return new UserVisibility(zones, identity == null ? IdentityType.VISITOR : identity);
    }

    @Override
    public Map<String, Object> myPerms(Long userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        String roleCode = user == null ? UserZoneMapping.DEFAULT_USER_ROLE : roleCodeOf(user);
        UserRole role = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleCode, roleCode));
        Set<String> zones = zonesOf(roleCode);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roleCode", roleCode);
        result.put("roleName", role == null ? roleCode : role.getRoleName());
        int identity = user == null ? IdentityType.VISITOR : campusRuleService.effectiveIdentityType(user);
        result.put("identityType", identity);
        result.put("identityLabel", IdentityType.label(identity));
        result.put("rawIdentityType", user == null ? null : user.getIdentityType());
        result.put("verified", user != null && isVerified(user));

        Map<String, String> zoneNames = zoneNameMap();
        List<Map<String, Object>> zoneList = new ArrayList<>();
        for (String code : zones) {
            Map<String, Object> z = new LinkedHashMap<>();
            z.put("code", code);
            z.put("name", zoneNames.getOrDefault(code, code));
            zoneList.add(z);
        }
        result.put("zones", zoneList);
        result.put("zoneNames", zoneList.stream().map(z -> z.get("name")).collect(Collectors.toList()));
        return result;
    }

    @Override
    public void changeUserRole(Long userId, String roleCode) {
        if (userId == null) throw new BusinessException("用户 ID 不能为空");
        if (!StringUtils.hasText(roleCode)) throw new BusinessException("角色编码不能为空");
        UserRole role = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleCode, roleCode.trim()));
        if (role == null) throw new BusinessException("角色不存在");
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        user.setRoleCode(role.getRoleCode());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public void syncRoleByIdentity(Long userId, Integer identityType) {
        if (userId == null) return;
        User user = userMapper.selectById(userId);
        if (user == null) return;
        user.setRoleCode(resolveRoleCodeByIdentity(identityType));
        userMapper.updateById(user);
    }

    @Override
    public void invalidate(String roleCode) {
        if (StringUtils.hasText(roleCode)) {
            cache.remove(roleCode);
        }
    }

    // ===================== 内部工具 =====================

    /** 角色可见区域（带 5 分钟缓存）；角色不存在时回退为默认访客角色的区域 */
    private Set<String> zonesOf(String roleCode) {
        String code = StringUtils.hasText(roleCode) ? roleCode : UserZoneMapping.DEFAULT_USER_ROLE;
        CachedZones c = cache.get(code);
        long now = System.currentTimeMillis();
        if (c != null && c.expireAt > now) {
            return c.zones;
        }
        UserRole role = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleCode, code));
        Set<String> zones;
        if (role == null) {
            // 未知角色：回退到访客角色的可见区域，避免用户端看到全部车位
            UserRole fallback = userRoleMapper.selectOne(
                    new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleCode, UserZoneMapping.DEFAULT_USER_ROLE));
            zones = fallback == null ? Collections.emptySet() : permsOf(fallback.getId());
        } else {
            zones = permsOf(role.getId());
        }
        cache.put(code, new CachedZones(zones, now + TTL_MS));
        return zones;
    }

    private Set<String> permsOf(Long roleId) {
        // 读路径不做区域存在性过滤：即使某个区域被误删，也不应连带屏蔽该角色已授权的车位
        return userRolePermMapper.selectList(
                        new LambdaQueryWrapper<UserRolePerm>().eq(UserRolePerm::getRoleId, roleId))
                .stream().map(UserRolePerm::getPermCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isVerified(User user) {
        return user.getIdentityStatus() != null
                && user.getIdentityStatus() == IdentityType.AUDIT_APPROVED;
    }

    private UserRole mustExist(Long id) {
        if (id == null) throw new BusinessException("角色 ID 不能为空");
        UserRole role = userRoleMapper.selectById(id);
        if (role == null) throw new BusinessException("角色不存在");
        return role;
    }

    /** 缓存条目 */
    private static class CachedZones {
        final Set<String> zones;
        final long expireAt;
        CachedZones(Set<String> zones, long expireAt) {
            this.zones = zones;
            this.expireAt = expireAt;
        }
    }
}
