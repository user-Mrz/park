package com.parking.service;

import com.parking.dto.AdminLoginDTO;
import com.parking.dto.LoginDTO;
import com.parking.dto.RegisterDTO;
import com.parking.dto.WechatLoginDTO;

import java.util.Map;

public interface AuthService {
    Map<String, Object> register(RegisterDTO dto);
    Map<String, Object> login(LoginDTO dto);
    Map<String, Object> adminLogin(AdminLoginDTO dto);
    Map<String, Object> wechatLogin(WechatLoginDTO dto);
}
