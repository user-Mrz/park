package com.parking.common.interceptor;

import com.parking.common.UserContext;
import com.parking.entity.User;
import com.parking.mapper.UserMapper;
import com.parking.util.JwtUtil;
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
            response.getWriter().write("{\"code\":401,\"message\":\"Not logged in\"}");
            return false;
        }
        token = token.substring(7);
        try {
            Claims claims = jwtUtil.parseToken(token);
            String role = claims.get("role", String.class);
            String requestUri = request.getRequestURI();
            if (requestUri.startsWith("/api/admin/") && !"admin".equals(role)) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"Admin permission required\"}");
                return false;
            }
            Long userId = Long.valueOf(claims.getSubject());
            // 非管理员请求时，检查用户是否被禁用
            if (!"admin".equals(role)) {
                User user = userMapper.selectById(userId);
                if (user == null || user.getStatus() == 0) {
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"Account has been disabled or deleted\"}");
                    return false;
                }
            }
            UserContext.setUserId(userId);
            UserContext.setPhone(claims.get("phone", String.class));
            UserContext.setRole(role);
            return true;
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Invalid or expired token\"}");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
