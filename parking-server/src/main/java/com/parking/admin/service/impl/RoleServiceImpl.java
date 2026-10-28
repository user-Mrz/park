package com.parking.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parking.common.BusinessException;
import com.parking.framework.interceptor.MenuAuthInterceptor;
import com.parking.common.menu.MenuPermissionMapping;
import com.parking.system.dto.RoleAssignMenusDTO;
import com.parking.system.dto.RoleSaveDTO;
import com.parking.system.domain.Admin;
import com.parking.system.domain.Role;
import com.parking.system.domain.RoleMenu;
import com.parking.system.mapper.AdminMapper;
import com.parking.system.mapper.RoleMapper;
import com.parking.system.mapper.RoleMenuMapper;
import com.parking.admin.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 后台角色（RBAC）管理实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    /** 角色编码：2-50 位字母/数字/下划线 */
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_]{2,50}$");

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final AdminMapper adminMapper;
    private final MenuAuthInterceptor menuAuthInterceptor;

    @Override
    public List<Map<String, Object>> listRoles(String keyword) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Role::getRoleCode, keyword).or().like(Role::getRoleName, keyword));
        }
        wrapper.orderByAsc(Role::getId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Role r : roleMapper.selectList(wrapper)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", String.valueOf(r.getId()));
            m.put("roleCode", r.getRoleCode());
            m.put("roleName", r.getRoleName());
            m.put("description", r.getDescription());
            m.put("builtin", r.getBuiltin());
            m.put("menus", menusOf(r));
            Long count = adminMapper.selectCount(
                    new LambdaQueryWrapper<Admin>().eq(Admin::getRole, r.getRoleCode()));
            m.put("adminCount", count == null ? 0 : count);
            m.put("createdAt", r.getCreatedAt());
            result.add(m);
        }
        return result;
    }

    @Override
    @Transactional
    public Role create(RoleSaveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getRoleCode())) {
            throw new BusinessException("角色编码不能为空");
        }
        String code = dto.getRoleCode().trim();
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new BusinessException("角色编码为 2-50 位字母、数字或下划线");
        }
        if (!StringUtils.hasText(dto.getRoleName())) {
            throw new BusinessException("角色名称不能为空");
        }
        if (existsByCode(code)) {
            throw new BusinessException("角色编码已存在");
        }
        Role role = new Role();
        role.setRoleCode(code);
        role.setRoleName(dto.getRoleName().trim());
        role.setDescription(dto.getDescription());
        role.setBuiltin(0);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.insert(role);
        return role;
    }

    @Override
    public Role update(Long id, RoleSaveDTO dto) {
        Role role = mustExist(id);
        if (dto == null || !StringUtils.hasText(dto.getRoleName())) {
            throw new BusinessException("角色名称不能为空");
        }
        // roleCode 不可变更：管理员账号以此为绑定键，改名会切断关联
        role.setRoleName(dto.getRoleName().trim());
        role.setDescription(dto.getDescription());
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.updateById(role);
        return role;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = mustExist(id);
        if (role.getBuiltin() != null && role.getBuiltin() == 1) {
            throw new BusinessException("内置角色不可删除");
        }
        Long count = adminMapper.selectCount(new LambdaQueryWrapper<Admin>().eq(Admin::getRole, role.getRoleCode()));
        if (count != null && count > 0) {
            throw new BusinessException("该角色下仍有 " + count + " 个管理员账号，请先调整账号角色");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id));
        menuAuthInterceptor.invalidate(role.getRoleCode());
    }

    @Override
    public List<String> getRoleMenus(Long roleId) {
        return menusOf(mustExist(roleId));
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, RoleAssignMenusDTO dto) {
        Role role = mustExist(roleId);
        if (MenuPermissionMapping.SUPER_ADMIN_ROLE.equals(role.getRoleCode())) {
            throw new BusinessException("超级管理员角色拥有全部权限，无需分配");
        }
        // 全量覆盖：先删后插
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
        if (dto != null && dto.getMenuCodes() != null) {
            LocalDateTime now = LocalDateTime.now();
            for (String code : dto.getMenuCodes()) {
                if (!StringUtils.hasText(code)) continue;
                // 超管专属菜单不允许分配给普通角色
                if (MenuPermissionMapping.isSuperOnly(code)) continue;
                RoleMenu rm = new RoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuCode(code);
                rm.setCreatedAt(now);
                roleMenuMapper.insert(rm);
            }
        }
        // 清缓存：下次请求重新拉最新权限
        menuAuthInterceptor.invalidate(role.getRoleCode());
    }

    @Override
    public List<String> getMenusByRoleCode(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return Collections.emptyList();
        }
        if (MenuPermissionMapping.SUPER_ADMIN_ROLE.equals(roleCode)) {
            return MenuPermissionMapping.allMenuCodes();
        }
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, roleCode));
        if (role == null) {
            return Collections.emptyList();
        }
        return menusOf(role);
    }

    @Override
    public Map<String, String> roleNames() {
        Map<String, String> m = new LinkedHashMap<>();
        for (Role r : roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId))) {
            m.put(r.getRoleCode(), r.getRoleName());
        }
        return m;
    }

    @Override
    public boolean existsByCode(String roleCode) {
        if (!StringUtils.hasText(roleCode)) return false;
        Long c = roleMapper.selectCount(new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, roleCode));
        return c != null && c > 0;
    }

    /** 计算角色的菜单 code 列表：超管为全部菜单，其余查 t_role_menu */
    private List<String> menusOf(Role role) {
        if (MenuPermissionMapping.SUPER_ADMIN_ROLE.equals(role.getRoleCode())) {
            return MenuPermissionMapping.allMenuCodes();
        }
        return roleMenuMapper.selectList(
                        new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, role.getId()))
                .stream().map(RoleMenu::getMenuCode).collect(Collectors.toList());
    }

    private Role mustExist(Long id) {
        if (id == null) throw new BusinessException("角色 ID 不能为空");
        Role role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException("角色不存在");
        return role;
    }
}
