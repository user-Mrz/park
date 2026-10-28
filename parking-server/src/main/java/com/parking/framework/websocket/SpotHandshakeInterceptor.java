package com.parking.framework.websocket;

import com.parking.common.user.UserVisibility;
import com.parking.system.domain.User;
import com.parking.system.mapper.UserMapper;
import com.parking.system.service.CampusRuleService;
import com.parking.system.service.UserRoleService;
import com.parking.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 车位推送 WebSocket 握手鉴权。
 *
 * <p>浏览器原生 WebSocket 无法设置 Authorization 请求头，因此 token 从查询参数
 * {@code /ws/spots?token=...} 读取；同时也兼容非浏览器客户端携带 Bearer 头。</p>
 *
 * <p>鉴权口径与 {@code JwtInterceptor} 保持一致：管理员令牌只读全量车位；
 * 小程序用户令牌需校验账号状态，并提前计算可见范围与白名单标记。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpotHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final UserRoleService userRoleService;
    private final CampusRuleService campusRuleService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (Exception e) {
            log.debug("车位推送握手失败：token 无效或已过期");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        boolean admin = "admin".equals(claims.get("userType", String.class));
        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put("userId", userId);
        attributes.put("admin", admin);
        attributes.put("tokenExpireAt", claims.getExpiration() == null ? 0L : claims.getExpiration().getTime());

        if (admin) {
            attributes.put("whitelisted", false);
            attributes.put("visibility", null);
            return true;
        }

        // 小程序用户：账号必须存在且未被禁用（与 JwtInterceptor 一致）
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() == 0) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        UserVisibility visibility = userRoleService.visibilityOf(userId);
        boolean whitelisted = campusRuleService.isWhitelisted(user.getPlateNumber(), userId);
        attributes.put("whitelisted", whitelisted);
        attributes.put("visibility", visibility);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }

    private String resolveToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            if (StringUtils.hasText(token)) {
                return token;
            }
            String authorization = servletRequest.getServletRequest().getHeader("Authorization");
            if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
                return authorization.substring(7);
            }
        }
        return null;
    }
}
