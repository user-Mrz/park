package com.parking.framework.config;

import com.parking.framework.websocket.SpotHandshakeInterceptor;
import com.parking.framework.websocket.SpotWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 车位状态实时推送 WebSocket 配置。
 *
 * <p>管理端与小程序共用 {@code /ws/spots}，鉴权由握手拦截器基于 JWT 完成；
 * 不使用 SockJS/STOMP，保持小程序端原生 {@code wx.connectSocket} 可直接接入。</p>
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class SpotWebSocketConfig implements WebSocketConfigurer {

    private final SpotWebSocketHandler spotWebSocketHandler;
    private final SpotHandshakeInterceptor spotHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(spotWebSocketHandler, "/ws/spots")
                .addInterceptors(spotHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
