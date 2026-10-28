package com.parking.miniapp.service;

import com.parking.system.dto.LoginDTO;
import com.parking.system.dto.RegisterDTO;
import com.parking.system.dto.WechatLoginDTO;

import java.util.Map;

/**
 * 小程序端认证服务：注册、手机号登录、微信登录。
 */
public interface AuthService {
    Map<String, Object> register(RegisterDTO dto);
    Map<String, Object> login(LoginDTO dto);
    Map<String, Object> wechatLogin(WechatLoginDTO dto);
}
