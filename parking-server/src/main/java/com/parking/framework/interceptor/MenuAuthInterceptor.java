package com.parking.framework.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parking.common.UserContext;
import com.parking.common.menu.MenuPermissionMapping;
import com.parking.system.domain.Role;
import com.parking.system.domain.RoleMenu;
import com.parking.system.mapper.RoleMapper;
import com.parking.system.mapper.RoleMenuMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 后台菜单权限拦截器（RBAC）：
 * <ul>
 *   <li>仅对 /api/admin/** 且 userType=admin 的请求生效</li>
 *   <li>超级管理员角色（role_code = super_admin）直接放行</li>
 *   <li>普通管理员：解析 URI → menuCode → 按「自身角色」查询角色菜单权限（5 分钟缓存）→ 超管专属 403；不在权限内 403</li>
 *   <li>公共路径（如 /account/my-menus）直接放行</li>
 * </ul>
 * <p>
 * 权限来源为 t_role_menu（角色-菜单），不再按账号单独分配；修改角色菜单后需调用
 * {@link #invalidate(String)} 清缓存。
 * <p>
 * 顺序：必须排在 {@link JwtInterceptor} 之后，依赖 UserContext 已设置 role/userType/userId。
 */
@Slf4j
@Component
public class MenuAuthInterceptor implements HandlerInterceptor {

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;

    /** 角色菜单权限缓存：roleCode -> { menus, expireAt }，TTL 5 分钟 */
    private final ConcurrentHashMap<String, CachedMenus> cache = new ConcurrentHashMap<>();
    private static final long TTL_MS = 5L * 60 * 1000;

    public MenuAuthInterceptor(RoleMapper roleMapper, RoleMenuMapper roleMenuMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestUri = request.getRequestURI();
        // 仅处理后台 API；非 admin 域由 JwtInterceptor 负责
        if (!requestUri.startsWith("/api/admin/")) {
            return true;
        }
        // 仅管理员令牌走菜单校验
        if (!"admin".equals(UserContext.getUserType())) {
            return true;
        }
        // 超管放行所有后台接口
        if (MenuPermissionMapping.SUPER_ADMIN_ROLE.equals(UserContext.getRole())) {
            return true;
        }
        Long adminId = UserContext.getUserId();
        if (adminId == null) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已失效\"}");
            return false;
        }

        // 解析菜单 code
        String menuCode = MenuPermissionMapping.resolveMenuCode(requestUri);
        // resolveMenuCode 返回 null 表示公共路径（如 /account/my-menus），放行
        if (menuCode == null) {
            return true;
        }

        // 超管专属菜单：普通管理员即使被分配到角色也拒绝
        if (MenuPermissionMapping.isSuperOnly(menuCode)) {
            return deny(response);
        }

        String roleCode = UserContext.getRole();
        if (!StringUtils.hasText(roleCode)) {
            return deny(response);
        }

        // 拿当前角色（RBAC）的菜单权限集合（带缓存）
        Set<String> allowed = getAllowedMenus(roleCode);
        if (!allowed.contains(menuCode)) {
            return deny(response);
        }
        return true;
    }

    /**
     * 获取指定角色的菜单权限集合（带 5 分钟缓存）。
     */
    private Set<String> getAllowedMenus(String roleCode) {
        CachedMenus c = cache.get(roleCode);
        long now = System.currentTimeMillis();
        if (c != null && c.expireAt > now) {
            return c.menus;
        }
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, roleCode));
        Set<String> menus = new HashSet<>();
        if (role != null) {
            List<RoleMenu> list = roleMenuMapper.selectList(
                    new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, role.getId()));
            menus = list.stream()
                    .map(RoleMenu::getMenuCode)
                    .collect(Collectors.toCollection(HashSet::new));
        }
        cache.put(roleCode, new CachedMenus(menus, now + TTL_MS));
        return menus;
    }

    /**
     * 在角色菜单权限变更后调用，清空指定角色的缓存（保证下次请求重新拉最新权限）。
     */
    public void invalidate(String roleCode) {
        if (StringUtils.hasText(roleCode)) {
            cache.remove(roleCode);
        }
    }

    /**
     * 清空全部角色权限缓存（角色被删除/重建等场景）。
     */
    public void invalidateAll() {
        cache.clear();
    }

    private boolean deny(HttpServletResponse response) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"无权访问该菜单\"}");
        return false;
    }

    /** 缓存条目 */
    private static class CachedMenus {
        final Set<String> menus;
        final long expireAt;
        CachedMenus(Set<String> menus, long expireAt) {
            this.menus = menus;
            this.expireAt = expireAt;
        }
    }
}
