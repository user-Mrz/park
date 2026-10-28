package com.parking.framework.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.common.user.UserVisibility;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 车位状态 WebSocket 会话注册表。
 *
 * <p>只负责会话的注册、发送与移除，不做业务过滤；按用户可见范围过滤的逻辑在
 * {@code SpotRealtimeService} 中完成。</p>
 *
 * <p>发送统一走 {@link ConcurrentWebSocketSessionDecorator}，避免定时任务与请求线程
 * 同时向同一连接发送消息导致 IllegalStateException。</p>
 */
@Slf4j
@Component
public class SpotSessionRegistry {

    /** 单次发送最长阻塞时间（毫秒） */
    private static final int SEND_TIME_LIMIT_MS = 10_000;
    /** 发送缓冲区上限（字节） */
    private static final int SEND_BUFFER_SIZE_LIMIT = 512 * 1024;

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, SpotSession> sessions = new ConcurrentHashMap<>();

    public SpotSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(WebSocketSession rawSession, Long userId, boolean admin,
                         boolean whitelisted, UserVisibility visibility, long tokenExpireAt) {
        WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(
                rawSession, SEND_TIME_LIMIT_MS, SEND_BUFFER_SIZE_LIMIT);
        SpotSession spotSession = new SpotSession(safeSession, userId, admin, whitelisted, visibility, tokenExpireAt);
        sessions.put(rawSession.getId(), spotSession);
        log.info("车位推送 WebSocket 连接建立：sessionId={}, admin={}, userId={}, 当前连接数={}",
                rawSession.getId(), admin, userId, sessions.size());
    }

    public SpotSession get(String sessionId) {
        return sessions.get(sessionId);
    }

    public Collection<SpotSession> all() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    public int size() {
        return sessions.size();
    }

    /** 收到客户端 pong 时刷新活跃时间 */
    public void touchPong(String sessionId) {
        SpotSession session = sessions.get(sessionId);
        if (session != null) {
            session.lastPongAt = System.currentTimeMillis();
        }
    }

    public void unregister(String sessionId) {
        SpotSession removed = sessions.remove(sessionId);
        if (removed != null) {
            log.info("车位推送 WebSocket 连接关闭：sessionId={}, admin={}, userId={}, 剩余连接数={}",
                    sessionId, removed.isAdmin(), removed.getUserId(), sessions.size());
        }
    }

    /**
     * 向单个会话发送 JSON 消息；发送失败时摘除会话，避免僵尸连接拖慢后续广播。
     */
    public boolean send(SpotSession session, Object payload) {
        try {
            session.getDelegate().sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            return true;
        } catch (Exception e) {
            log.debug("车位推送发送失败，移除会话 {}：{}", session.getDelegate().getId(), e.getMessage());
            sessions.remove(session.getDelegate().getId(), session);
            closeQuietly(session);
            return false;
        }
    }

    /** 发送一条错误消息后关闭连接（用于登录过期 / 心跳超时） */
    public void sendErrorAndClose(SpotSession session, int code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("type", "error");
        error.put("code", code);
        error.put("message", message);
        send(session, error);
        close(session, code, message);
    }

    public void close(SpotSession session, int code, String reason) {
        sessions.remove(session.getDelegate().getId(), session);
        try {
            session.getDelegate().close(new CloseStatus(code, reason));
        } catch (Exception e) {
            log.debug("关闭车位推送会话 {} 失败：{}", session.getDelegate().getId(), e.getMessage());
        }
    }

    private void closeQuietly(SpotSession session) {
        try {
            session.getDelegate().close(CloseStatus.SERVER_ERROR);
        } catch (Exception ignored) {
            // 连接可能已断开，忽略
        }
    }

    /**
     * 一个已鉴权的车位推送会话。
     */
    @Getter
    public static class SpotSession {
        private final WebSocketSession delegate;
        private final Long userId;
        private final boolean admin;
        private final boolean whitelisted;
        private final UserVisibility visibility;
        /** token 过期时间戳（毫秒）；管理员与用户都需要在到期后断开 */
        private final long tokenExpireAt;
        /** 最近一次收到 pong 的时间戳（毫秒） */
        private volatile long lastPongAt;
        /** 最近一次 snapshot 过滤后该会话可见的车位 ID，用于 patch 过滤 */
        private final Set<Long> visibleSpotIds = ConcurrentHashMap.newKeySet();

        public SpotSession(WebSocketSession delegate, Long userId, boolean admin, boolean whitelisted,
                           UserVisibility visibility, long tokenExpireAt) {
            this.delegate = delegate;
            this.userId = userId;
            this.admin = admin;
            this.whitelisted = whitelisted;
            this.visibility = visibility;
            this.tokenExpireAt = tokenExpireAt;
            this.lastPongAt = System.currentTimeMillis();
        }
    }
}
