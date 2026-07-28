package com.parking.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parking.common.BusinessException;
import com.parking.dto.AdminLoginDTO;
import com.parking.dto.LoginDTO;
import com.parking.dto.RegisterDTO;
import com.parking.dto.WechatLoginDTO;
import com.parking.entity.Admin;
import com.parking.entity.User;
import com.parking.mapper.AdminMapper;
import com.parking.mapper.UserMapper;
import com.parking.service.AuthService;
import com.parking.service.CaptchaService;
import com.parking.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final JwtUtil jwtUtil;

    @Override
    public Map<String, Object> register(RegisterDTO dto) {
        if (!captchaService.validateCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode())) {
            throw new BusinessException("Invalid captcha code");
        }
        User exist = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (exist != null) {
            throw new BusinessException("Phone number already registered");
        }
        Snowflake sf = IdUtil.getSnowflake(1, 1);
        User user = new User();
        user.setId(sf.nextId());
        user.setPhone(dto.getPhone());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getPhone());
        user.setPlateNumber(dto.getPlateNumber() != null ? dto.getPlateNumber() : "");
        user.setBalance(BigDecimal.ZERO);
        user.setStatus(1);
        userMapper.insert(user);
        String token = jwtUtil.generateToken(user.getId(), user.getPhone(), "user");
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("phone", user.getPhone());
        result.put("nickname", user.getNickname());
        result.put("plateNumber", user.getPlateNumber());
        result.put("balance", user.getBalance());
        return result;
    }

    @Override
    public Map<String, Object> login(LoginDTO dto) {
        if (!captchaService.validateCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode())) {
            throw new BusinessException("Invalid captcha code");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (user == null) {
            throw new BusinessException("Phone number not registered");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("Account has been disabled");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("Incorrect password");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getPhone(), "user");
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("phone", user.getPhone());
        result.put("nickname", user.getNickname());
        result.put("plateNumber", user.getPlateNumber());
        result.put("balance", user.getBalance());
        return result;
    }

    @Override
    public Map<String, Object> adminLogin(AdminLoginDTO dto) {
        if (!captchaService.validateCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode())) {
            throw new BusinessException("Invalid captcha code");
        }
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, dto.getUsername()));
        if (admin == null) {
            throw new BusinessException("Admin account not found");
        }
        if (admin.getStatus() == 0) {
            throw new BusinessException("Admin account has been disabled");
        }
        if (!passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            throw new BusinessException("Incorrect password");
        }
        String token = jwtUtil.generateToken(admin.getId(), admin.getUsername(), "admin");
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("adminId", admin.getId());
        result.put("username", admin.getUsername());
        result.put("nickname", admin.getNickname());
        result.put("role", admin.getRole());
        return result;
    }

    @Override
    public Map<String, Object> wechatLogin(WechatLoginDTO dto) {
        // In production, call WeChat API to get openid with dto.getCode()
        // For dev/demo, mock the openid
        String mockOpenid = "wx_" + dto.getCode();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, mockOpenid));
        if (user == null) {
            Snowflake sf = IdUtil.getSnowflake(1, 1);
            user = new User();
            user.setId(sf.nextId());
            user.setOpenid(mockOpenid);
            user.setNickname(dto.getNickname() != null ? dto.getNickname() : "WeChat User");
            user.setBalance(BigDecimal.ZERO);
            user.setStatus(1);
            userMapper.insert(user);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("Account has been disabled");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getPhone() != null ? user.getPhone() : "", "user");
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("phone", user.getPhone());
        result.put("nickname", user.getNickname());
        result.put("plateNumber", user.getPlateNumber());
        result.put("balance", user.getBalance());
        return result;
    }
}
