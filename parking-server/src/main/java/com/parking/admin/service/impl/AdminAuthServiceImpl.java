package com.parking.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parking.admin.service.AdminAuthService;
import com.parking.admin.service.RoleService;
import com.parking.common.BusinessException;
import com.parking.common.util.JwtUtil;
import com.parking.system.domain.Admin;
import com.parking.system.domain.AdminLoginLog;
import com.parking.system.dto.AdminLoginDTO;
import com.parking.system.mapper.AdminLoginLogMapper;
import com.parking.system.mapper.AdminMapper;
import com.parking.system.service.CaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理端认证实现：验证码校验、账号状态校验、BCrypt 密码校验、JWT 签发与登录日志落库。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminMapper adminMapper;
    private final AdminLoginLogMapper adminLoginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final JwtUtil jwtUtil;
    private final RoleService roleService;

    @Override
    public Map<String, Object> adminLogin(AdminLoginDTO dto) {
        if (!captchaService.validateCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode())) {
            // 验证码错误：admin 尚未查出，adminId 为 null，username 记录用户输入
            writeLoginLog(null, dto.getUsername(), "FAIL", "CAPTCHA_INVALID");
            throw new BusinessException("验证码错误");
        }
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, dto.getUsername()));
        if (admin == null) {
            writeLoginLog(null, dto.getUsername(), "FAIL", "ACCOUNT_NOT_FOUND");
            throw new BusinessException("管理员账号不存在");
        }
        if (admin.getStatus() == 0) {
            writeLoginLog(admin.getId(), admin.getUsername(), "FAIL", "ACCOUNT_DISABLED");
            throw new BusinessException("管理员账号已被禁用");
        }
        if (!passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            writeLoginLog(admin.getId(), admin.getUsername(), "FAIL", "PASSWORD_INCORRECT");
            throw new BusinessException("密码错误");
        }
        String token = jwtUtil.generateAdminToken(admin.getId(), admin.getUsername(), admin.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("adminId", admin.getId());
        result.put("username", admin.getUsername());
        result.put("nickname", admin.getNickname());
        // RBAC：role 为角色编码，roleName 为角色名称
        result.put("role", admin.getRole());
        result.put("roleName", roleService.roleNames().getOrDefault(admin.getRole(), admin.getRole()));
        // 成功登录日志
        writeLoginLog(admin.getId(), admin.getUsername(), "SUCCESS", null);
        return result;
    }

    /**
     * 写一条登录日志。失败时吞异常，避免影响登录主流程。
     * <p>
     * 字段：
     * - adminId    账号不存在时为 null
     * - username   用户输入或账号用户名
     * - status     SUCCESS / FAIL
     * - reason     CAPTCHA_INVALID / ACCOUNT_NOT_FOUND / ACCOUNT_DISABLED / PASSWORD_INCORRECT
     * - ip         X-Forwarded-For 优先，否则 RemoteAddr
     * - userAgent  截断 500
     */
    private void writeLoginLog(Long adminId, String username, String status, String reason) {
        try {
            AdminLoginLog entity = new AdminLoginLog();
            entity.setAdminId(adminId);
            entity.setUsername(username);
            entity.setLoginStatus(status);
            entity.setFailReason(reason);
            HttpServletRequest req = currentRequest();
            if (req != null) {
                entity.setIp(resolveIp(req));
                String ua = req.getHeader("User-Agent");
                if (ua != null && ua.length() > 500) ua = ua.substring(0, 500);
                entity.setUserAgent(ua);
            }
            entity.setLoginTime(LocalDateTime.now());
            adminLoginLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("Write admin login log failed: {}", e.getMessage());
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String resolveIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            int comma = ip.indexOf(',');
            if (comma > 0) ip = ip.substring(0, comma).trim();
            return ip;
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) return ip;
        return request.getRemoteAddr();
    }
}
