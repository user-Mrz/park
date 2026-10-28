package com.parking.framework.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.common.user.UserVisibility;
import com.parking.system.service.SpotRealtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * 车位状态推送 WebSocket 处理器。
 *
 * <p>连接建立后立即发送一次 snapshot；之后只处理客户端的 pong 与 resync，
 * 其他消息一律忽略。定时心跳与广播由 {@link SpotRealtimeService} 负责。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpotWebSocketHandler extends TextWebSocketHandler {

    private final SpotSessionRegistry sessionRegistry;
    private final SpotRealtimeService spotRealtimeService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();
        Long userId = attributes.get("userId") instanceof Long id ? id : null;
        boolean admin = Boolean.TRUE.equals(attributes.get("admin"));
        boolean whitelisted = Boolean.TRUE.equals(attributes.get("whitelisted"));
        UserVisibility visibility = attributes.get("visibility") instanceof UserVisibility vis ? vis : null;
        long tokenExpireAt = attributes.get("tokenExpireAt") instanceof Long exp ? exp : 0L;

        sessionRegistry.register(session, userId, admin, whitelisted, visibility, tokenExpireAt);
        SpotSessionRegistry.SpotSession registered = sessionRegistry.get(session.getId());
        if (registered != null) {
            spotRealtimeService.sendSnapshotTo(registered);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            String type = node.path("type").asText("");
            if ("pong".equals(type)) {
                sessionRegistry.touchPong(session.getId());
                return;
            }
            if ("resync".equals(type)) {
                SpotSessionRegistry.SpotSession spotSession = sessionRegistry.get(session.getId());
                if (spotSession != null) {
                    spotRealtimeService.sendSnapshotTo(spotSession);
                }
            }
        } catch (Exception e) {
            log.debug("忽略无法解析的车位推送消息：{}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("车位推送连接异常 sessionId={}：{}", session.getId(), exception.getMessage());
        sessionRegistry.unregister(session.getId());
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (Exception ignored) {
            // 连接已断开，忽略
        }
    }
}
