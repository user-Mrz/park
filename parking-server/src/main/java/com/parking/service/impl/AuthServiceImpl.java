package com.parking.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parking.common.BusinessException;
import com.parking.common.IdentityType;
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
import com.parking.service.DepartmentService;
import com.parking.util.JwtUtil;
import com.parking.util.WechatUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private final DepartmentService departmentService;

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

        // ---- 校园身份：学生/教师/职工需填写学号工号并等待管理员审核；访客/家长直接可用 ----
        int identityType = dto.getIdentityType() == null ? IdentityType.STUDENT : dto.getIdentityType();
        if (identityType < IdentityType.STUDENT || identityType > IdentityType.PARENT) {
            throw new BusinessException("身份类型不合法");
        }
        String userNo = dto.getUserNo() == null ? "" : dto.getUserNo().trim();
        if (identityType == IdentityType.STUDENT || identityType == IdentityType.TEACHER
                || identityType == IdentityType.STAFF) {
            if (!StringUtils.hasText(userNo)) throw new BusinessException("请填写学号/工号");
            if (!userNo.matches("^[A-Za-z0-9]{4,20}$")) {
                throw new BusinessException("学号/工号格式不正确（4-20 位字母或数字）");
            }
            Long dup = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUserNo, userNo));
            if (dup != null && dup > 0) throw new BusinessException("该学号/工号已被注册");
            user.setUserNo(userNo);
            user.setIdentityStatus(IdentityType.AUDIT_PENDING);
            user.setIdentityRemark("等待管理员审核学号/工号");
        } else {
            user.setUserNo(null);
            user.setIdentityStatus(IdentityType.AUDIT_APPROVED);
            user.setIdentityRemark("");
        }
        user.setIdentityType(identityType);
        // 学院/部门：学生、教师、职工必须从「学院管理」维护的列表中选择
        String department = dto.getDepartment() == null ? "" : dto.getDepartment().trim();
        if (identityType <= IdentityType.STAFF && !departmentService.isValidName(department)) {
            throw new BusinessException("请选择有效的学院/部门");
        }
        user.setDepartment(department);
        user.setClassName(dto.getClassName() == null ? "" : dto.getClassName().trim());
        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId(), user.getPhone(), "user");
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("phone", user.getPhone());
        result.put("nickname", user.getNickname());
        result.put("plateNumber", user.getPlateNumber());
        result.put("balance", user.getBalance());
        result.put("userNo", user.getUserNo());
        result.put("identityType", user.getIdentityType());
        result.put("identityStatus", user.getIdentityStatus());
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
        // In production, call WeChat API (code2session) to get openid with dto.getCode()
        // For dev/demo, mock the openid.
        // 使用小程序传入的稳定设备标识生成 mock openid，避免 code 每次变化导致
        // 同一用户每次登录都被当成新用户（产生重复账号，并可绕过账号禁用）。
        String deviceId = dto.getDeviceId();
        String mockOpenid = WechatUtil.resolveOpenid(dto.getCode(), deviceId);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, mockOpenid));
        if (user == null) {
            Snowflake sf = IdUtil.getSnowflake(1, 1);
            user = new User();
            user.setId(sf.nextId());
            user.setOpenid(mockOpenid);
            user.setNickname(dto.getNickname() != null ? dto.getNickname() : "WeChat User");
            // 微信用户尚未绑定手机号: 显式置 NULL(配合 t_user.phone 列默认值 NULL),
            // 避免多用户都落到列默认 '' 而触发 uk_phone 唯一索引冲突 (Bug-1)
            user.setPhone(null);
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
