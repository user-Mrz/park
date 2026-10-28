package com.parking.miniapp.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parking.common.BusinessException;
import com.parking.common.IdentityType;
import com.parking.common.util.JwtUtil;
import com.parking.common.util.WechatUtil;
import com.parking.miniapp.service.AuthService;
import com.parking.miniapp.service.CampusVerifyService;
import com.parking.system.domain.User;
import com.parking.system.dto.CampusVerifyResult;
import com.parking.system.dto.LoginDTO;
import com.parking.system.dto.RegisterDTO;
import com.parking.system.dto.WechatLoginDTO;
import com.parking.system.mapper.UserMapper;
import com.parking.system.service.CaptchaService;
import com.parking.system.service.DepartmentService;
import com.parking.system.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 小程序端认证实现：注册、手机号登录、微信登录。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final JwtUtil jwtUtil;
    private final DepartmentService departmentService;
    private final UserRoleService userRoleService;
    private final CampusVerifyService campusVerifyService;

    /** 微信登录 mock 开关：本地演示为 true；接入真实 code2session 后生产环境应设为 false */
    @Value("${parking.wechat.mock-login-enabled:true}")
    private boolean mockLoginEnabled;

    @Override
    public Map<String, Object> register(RegisterDTO dto) {
        if (!captchaService.validateCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode())) {
            throw new BusinessException("验证码错误");
        }
        User exist = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (exist != null) {
            throw new BusinessException("该手机号已注册");
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
        // 用户端 RBAC：按校园身份自动映射默认角色（后台可再微调）
        user.setRoleCode(userRoleService.resolveRoleCodeByIdentity(identityType));
        // 学院/部门：学生、教师、职工必须从「学院管理」维护的列表中选择
        String department = dto.getDepartment() == null ? "" : dto.getDepartment().trim();
        if (identityType <= IdentityType.STAFF && !departmentService.isValidName(department)) {
            throw new BusinessException("请选择有效的学院/部门");
        }
        user.setDepartment(department);
        user.setClassName(dto.getClassName() == null ? "" : dto.getClassName().trim());

        // 校内身份自动核验：校园系统查询通过则免人工审核，查不到或接口不可用时回落人工审核
        if (identityType <= IdentityType.STAFF) {
            CampusVerifyResult verify = campusVerifyService.verify(userNo, identityType);
            if (verify.matched()) {
                user.setIdentityStatus(IdentityType.AUDIT_APPROVED);
                user.setIdentityRemark("校园管理系统自动核验通过");
                if (StringUtils.hasText(verify.department())) user.setDepartment(verify.department());
                if (StringUtils.hasText(verify.className())) user.setClassName(verify.className());
            } else if (verify.status() == CampusVerifyResult.Status.NOT_FOUND) {
                user.setIdentityRemark("校园管理系统未查询到该学号/工号，等待管理员审核");
            } else {
                user.setIdentityRemark("校园管理系统暂不可用，等待管理员审核");
            }
        }
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
            throw new BusinessException("验证码错误");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (user == null) {
            throw new BusinessException("该手机号未注册");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
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
    public Map<String, Object> wechatLogin(WechatLoginDTO dto) {
        // mock 登录以客户端 deviceId 作为身份凭据，知道 deviceId 即可登录对应账号；
        // 生产环境必须接入 code2session，因此默认关闭该入口。
        if (!mockLoginEnabled) {
            throw new BusinessException("微信一键登录暂未接入正式渠道，请使用手机号 + 密码登录");
        }
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
            // 微信登录新建用户尚无校园身份，按默认访客角色处理
            user.setRoleCode(userRoleService.resolveRoleCodeByIdentity(IdentityType.VISITOR));
            userMapper.insert(user);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
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
