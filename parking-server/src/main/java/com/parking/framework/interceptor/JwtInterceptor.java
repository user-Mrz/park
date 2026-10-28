package com.parking.framework.interceptor;

import com.parking.common.UserContext;
import com.parking.system.domain.User;
import com.parking.system.mapper.UserMapper;
import com.parking.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public JwtInterceptor(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已失效\"}");
            return false;
        }
        token = token.substring(7);
        try {
            Claims claims = jwtUtil.parseToken(token);
            String role = claims.get("role", String.class);
            String userType = claims.get("userType", String.class);
            boolean isAdmin = "admin".equals(userType);
            String requestUri = request.getRequestURI();
            // 后台 /api/admin/** 接口仅允许后台管理员令牌（userType=admin）访问
            // 小程序用户令牌（userType=user）一律 403
            if (requestUri.startsWith("/api/admin/") && !isAdmin) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"需要管理员权限\"}");
                return false;
            }
            Long userId = Long.valueOf(claims.getSubject());
            // 小程序用户请求时，检查用户是否被禁用
            if (!isAdmin) {
                User user = userMapper.selectById(userId);
                if (user == null || user.getStatus() == 0) {
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"账号已被禁用或删除\"}");
                    return false;
                }
            }
            UserContext.setUserId(userId);
            UserContext.setPhone(claims.get("phone", String.class));
            UserContext.setRole(role);
            UserContext.setUserType(isAdmin ? "admin" : "user");
            return true;
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录凭证无效或已过期\"}");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
