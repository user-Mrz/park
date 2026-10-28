package com.parking.system.service;

import com.parking.common.user.UserVisibility;
import com.parking.system.dto.UserRolePermDTO;
import com.parking.system.dto.UserRoleUpdateDTO;
import com.parking.system.domain.User;
import com.parking.system.domain.UserRole;

import java.util.List;
import java.util.Map;

/**
 * 小程序用户端角色（RBAC）服务：角色 -> 可见停车区域（权限）。
 * <p>用户角色与后台管理员角色（{@code t_role}）完全分离。</p>
 */
public interface UserRoleService {

    /** 角色列表（含可见区域、账号数） */
    List<Map<String, Object>> listRoles(String keyword);

    /** 更新角色名称/描述（编码与身份类型不可改） */
    UserRole updateRole(Long id, UserRoleUpdateDTO dto);

    /** 角色可见区域 code 列表 */
    List<String> getRolePerms(Long roleId);

    /** 为用户角色分配可见区域（全量覆盖），并清缓存 */
    void assignPerms(Long roleId, UserRolePermDTO dto);

    /** 角色编码 -> 角色名称 */
    Map<String, String> roleNames();

    /** 停车区域选项（code -> 中文名），供前端「可见区域」选择框展示 */
    Map<String, String> zoneOptions();

    /** 按校园身份解析默认角色编码（注册时自动映射） */
    String resolveRoleCodeByIdentity(Integer identityType);

    /** 计算某用户的可见范围（角色可见区域 + 用于放行判定的生效身份） */
    UserVisibility visibilityOf(Long userId);

    /** 某用户所属角色编码（未设置时按校园身份推导） */
    String roleCodeOf(User user);

    /** 当前用户的角色与可见区域信息（供小程序展示） */
    Map<String, Object> myPerms(Long userId);

    /** 后台调整用户所属角色 */
    void changeUserRole(Long userId, String roleCode);

    /** 管理员修改用户校园身份后同步其角色（用户未自定义角色时） */
    void syncRoleByIdentity(Long userId, Integer identityType);

    /** 角色可见区域变更后清缓存 */
    void invalidate(String roleCode);
}
