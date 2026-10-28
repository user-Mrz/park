package com.parking.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.system.dto.AdminAccountCreateDTO;
import com.parking.system.dto.AdminAccountUpdateDTO;
import com.parking.system.dto.AdminResetPasswordDTO;
import com.parking.system.domain.Admin;

import java.util.List;
import java.util.Map;

/**
 * 后台管理员账号管理（超管专用）。
 * <p>
 * 权限采用 RBAC：账号只负责绑定角色（t_role），菜单权限由角色决定，见 {@link RoleService}。
 */
public interface AdminAccountService {

    /** 分页列表 */
    Page<Admin> listAccounts(int page, int pageSize, String keyword, Integer status);

    /** 创建管理员账号（绑定角色） */
    Admin create(AdminAccountCreateDTO dto);

    /** 修改昵称与角色 */
    Admin update(Long id, AdminAccountUpdateDTO dto);

    /** 删除账号（不能删超管、不能自删） */
    void delete(Long id);

    /** 重置密码 */
    void resetPassword(Long id, AdminResetPasswordDTO dto);

    /** 启停 */
    void updateStatus(Long id, Integer status);

    /** 当前登录管理员自己的菜单 code 列表（超管返回全部，其他返回所属角色的菜单） */
    List<String> getMyMenus();

    // ---- 个人中心（任意角色的管理员操作自己，不受菜单权限限制） ----

    /** 当前登录管理员的个人资料 */
    Map<String, Object> getMyProfile();

    /** 修改自己的昵称 */
    void updateMyNickname(String nickname);

    /** 修改自己的登录密码（必须校验原密码） */
    void changeMyPassword(String oldPassword, String newPassword);
}
