package com.example.oauth2sso.config;

import com.example.oauth2sso.handler.LogoutWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 配置WebSocket端点和处理器
 * 
 * @author Luowenqiang
 * @version 1.0.0
 * @since 2024-12-26
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private LogoutWebSocketHandler logoutWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册WebSocket处理器
        registry.addHandler(logoutWebSocketHandler, "/ws/logout")
                .setAllowedOrigins("*") // 生产环境中应该限制具体域名
                .withSockJS(); // 启用SockJS支持，提供更好的兼容性
    }
} 