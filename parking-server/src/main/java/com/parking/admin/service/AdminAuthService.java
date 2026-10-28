package com.parking.admin.service;

import com.parking.system.dto.AdminLoginDTO;

import java.util.Map;

/**
 * 管理端认证服务：管理员账号登录与登录日志。
 */
public interface AdminAuthService {

    Map<String, Object> adminLogin(AdminLoginDTO dto);
}
