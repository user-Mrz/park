package com.parking.admin.service;

import com.parking.system.dto.RoleAssignMenusDTO;
import com.parking.system.dto.RoleSaveDTO;
import com.parking.system.domain.Role;

import java.util.List;
import java.util.Map;

/**
 * 后台角色（RBAC）管理服务。
 * <p>
 * 权限模型：管理员账号（t_admin）绑定角色（t_role.role_code），角色持有菜单权限（t_role_menu）。
 */
public interface RoleService {

    /** 角色列表（含每个角色的菜单权限与账号数量） */
    List<Map<String, Object>> listRoles(String keyword);

    /** 新建角色（roleCode 唯一） */
    Role create(RoleSaveDTO dto);

    /** 修改角色（仅名称与描述，roleCode 不可变更） */
    Role update(Long id, RoleSaveDTO dto);

    /** 删除角色（内置角色不可删；角色下仍有账号时不可删） */
    void delete(Long id);

    /** 查询指定角色已分配的菜单 code 列表 */
    List<String> getRoleMenus(Long roleId);

    /** 全量覆盖分配角色菜单权限 */
    void assignMenus(Long roleId, RoleAssignMenusDTO dto);

    /** 按角色编码查询其菜单 code 列表（超管返回全部） */
    List<String> getMenusByRoleCode(String roleCode);

    /** 角色编码 -> 角色名称 映射 */
    Map<String, String> roleNames();

    /** 角色编码是否存在 */
    boolean existsByCode(String roleCode);
}
