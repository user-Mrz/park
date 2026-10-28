package com.parking.admin.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.common.BusinessException;
import com.parking.common.UserContext;
import com.parking.common.menu.MenuPermissionMapping;
import com.parking.system.dto.AdminAccountCreateDTO;
import com.parking.system.dto.AdminAccountUpdateDTO;
import com.parking.system.dto.AdminResetPasswordDTO;
import com.parking.system.domain.Admin;
import com.parking.system.mapper.AdminMapper;
import com.parking.admin.service.AdminAccountService;
import com.parking.admin.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理员账号管理（超管专用）实现。
 * <p>
 * RBAC 模型下账号不再直接持有菜单：只绑定角色编码（t_admin.role = t_role.role_code）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAccountServiceImpl implements AdminAccountService {

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    @Override
    public Page<Admin> listAccounts(int page, int pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Admin::getUsername, keyword).or().like(Admin::getNickname, keyword));
        }
        if (status != null) {
            wrapper.eq(Admin::getStatus, status);
        }
        wrapper.orderByDesc(Admin::getCreatedAt);
        return adminMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    @Transactional
    public Admin create(AdminAccountCreateDTO dto) {
        // 用户名唯一性校验
        Admin exist = adminMapper.selectOne(new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, dto.getUsername()));
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }
        // 角色校验：未指定时落到默认普通管理员角色
        String roleCode = StringUtils.hasText(dto.getRoleCode())
                ? dto.getRoleCode().trim()
                : MenuPermissionMapping.DEFAULT_ADMIN_ROLE;
        // 超管角色不可通过账号管理创建，避免越权
        if (MenuPermissionMapping.SUPER_ADMIN_ROLE.equals(roleCode)) {
            throw new BusinessException("不允许创建超级管理员账号");
        }
        if (!roleService.existsByCode(roleCode)) {
            throw new BusinessException("角色不存在：" + roleCode);
        }
        Snowflake sf = IdUtil.getSnowflake(1, 1);
        Admin admin = new Admin();
        admin.setId(sf.nextId());
        admin.setUsername(dto.getUsername());
        admin.setPassword(passwordEncoder.encode(dto.getPassword()));
        admin.setNickname(dto.getNickname());
        admin.setRole(roleCode);
        admin.setStatus(1);
        admin.setCreatedAt(LocalDateTime.now());
        adminMapper.insert(admin);
        return admin;
    }

    @Override
    public Admin update(Long id, AdminAccountUpdateDTO dto) {
        Admin admin = mustExist(id);
        admin.setNickname(dto.getNickname());
        // 可选：调整角色
        if (StringUtils.hasText(dto.getRoleCode())) {
            String roleCode = dto.getRoleCode().trim();
            if (!roleCode.equals(admin.getRole())) {
                if (MenuPermissionMapping.SUPER_ADMIN_ROLE.equals(admin.getRole())) {
                    throw new BusinessException("超级管理员账号不允许变更角色");
                }
                if (MenuPermissionMapping.SUPER_ADMIN_ROLE.equals(roleCode)) {
                    throw new BusinessException("不允许将账号设置为超级管理员");
                }
                if (!roleService.existsByCode(roleCode)) {
                    throw new BusinessException("角色不存在：" + roleCode);
                }
                admin.setRole(roleCode);
            }
        }
        adminMapper.updateById(admin);
        return admin;
    }

    @Override
    public void delete(Long id) {
        Admin admin = mustExist(id);
        if (MenuPermissionMapping.SUPER_ADMIN_ROLE.equals(admin.getRole())) {
            throw new BusinessException("不能删除超级管理员账号");
        }
        // 不允许自删
        Long currentId = UserContext.getUserId();
        if (currentId != null && currentId.equals(id)) {
            throw new BusinessException("不能删除当前登录的账号");
        }
        adminMapper.deleteById(id);
    }

    @Override
    public void resetPassword(Long id, AdminResetPasswordDTO dto) {
        Admin admin = mustExist(id);
        admin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        adminMapper.updateById(admin);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("账号状态取值仅支持 0（禁用）或 1（启用）");
        }
        Admin admin = mustExist(id);
        // 不允许停用当前登录的账号
        Long currentId = UserContext.getUserId();
        if (status == 0 && currentId != null && currentId.equals(id)) {
            throw new BusinessException("不能停用当前登录的账号");
        }
        admin.setStatus(status);
        adminMapper.updateById(admin);
    }

    @Override
    public List<String> getMyMenus() {
        Long adminId = UserContext.getUserId();
        String role = UserContext.getRole();
        if (adminId == null) {
            return Collections.emptyList();
        }
        // 权限由角色决定（RBAC）
        if (MenuPermissionMapping.SUPER_ADMIN_ROLE.equals(role)) {
            return MenuPermissionMapping.allMenuCodes();
        }
        return roleService.getMenusByRoleCode(role);
    }

    // ---- 个人中心（任意角色管理员自助，不受菜单权限限制） ----

    @Override
    public Map<String, Object> getMyProfile() {
        Admin admin = currentAdmin();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", String.valueOf(admin.getId()));
        m.put("username", admin.getUsername());
        m.put("nickname", admin.getNickname());
        m.put("roleCode", admin.getRole());
        m.put("roleName", roleService.roleNames().getOrDefault(admin.getRole(), admin.getRole()));
        m.put("status", admin.getStatus());
        m.put("createdAt", admin.getCreatedAt());
        return m;
    }

    @Override
    public void updateMyNickname(String nickname) {
        String trimmed = nickname == null ? "" : nickname.trim();
        if (trimmed.isEmpty()) throw new BusinessException("昵称不能为空");
        if (trimmed.length() > 32) throw new BusinessException("昵称不能超过 32 个字符");
        Admin admin = currentAdmin();
        admin.setNickname(trimmed);
        adminMapper.updateById(admin);
    }

    @Override
    public void changeMyPassword(String oldPassword, String newPassword) {
        if (!StringUtils.hasText(oldPassword)) throw new BusinessException("请输入原密码");
        String pwd = newPassword == null ? "" : newPassword.trim();
        if (pwd.length() < 6 || pwd.length() > 50) throw new BusinessException("新密码长度需为 6-50 位");
        if (pwd.equals(oldPassword)) throw new BusinessException("新密码不能与原密码相同");
        Admin admin = currentAdmin();
        // 自助改密必须校验原密码，避免 token 被窃取后直接改掉登录凭据
        if (!StringUtils.hasText(admin.getPassword()) || !passwordEncoder.matches(oldPassword, admin.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        admin.setPassword(passwordEncoder.encode(pwd));
        adminMapper.updateById(admin);
        log.info("管理员修改密码成功 adminId={} username={}", admin.getId(), admin.getUsername());
    }

    /** 取当前登录管理员，不存在时抛出业务异常 */
    private Admin currentAdmin() {
        Long id = UserContext.getUserId();
        if (id == null) throw new BusinessException(401, "未登录或登录已失效");
        Admin admin = adminMapper.selectById(id);
        if (admin == null) throw new BusinessException("管理员账号不存在");
        return admin;
    }

    private Admin mustExist(Long id) {
        if (id == null) throw new BusinessException("管理员 ID 不能为空");
        Admin admin = adminMapper.selectById(id);
        if (admin == null) throw new BusinessException("管理员账号不存在");
        return admin;
    }
}
