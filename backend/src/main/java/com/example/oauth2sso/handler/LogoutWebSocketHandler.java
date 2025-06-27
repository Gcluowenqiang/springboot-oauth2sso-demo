package com.example.oauth2sso.handler;

import com.example.oauth2sso.service.LogoutNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.servlet.http.HttpSession;

/**
 * 登出WebSocket处理器
 * 处理WebSocket连接、消息和断开连接事件
 * 
 * @author Luowenqiang
 * @version 1.0.0
 * @since 2024-12-26
 */
@Component
public class LogoutWebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(LogoutWebSocketHandler.class);
    
    @Autowired
    private LogoutNotificationService logoutNotificationService;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket连接建立: sessionId={}", session.getId());
        
        // 获取HTTP会话信息
        String httpSessionId = extractHttpSessionId(session);
        String username = extractUsername(session);
        
        if (httpSessionId != null && username != null) {
            // 注册WebSocket会话
            logoutNotificationService.registerWebSocketSession(httpSessionId, username, session);
            logger.info("WebSocket会话注册成功: httpSessionId={}, username={}, wsId={}", 
                       httpSessionId, username, session.getId());
        } else {
            logger.warn("无法获取用户信息，WebSocket连接可能无效: wsId={}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("无法获取用户信息"));
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.debug("收到WebSocket消息: wsId={}, message={}", session.getId(), payload);
        
        // 处理客户端发送的消息
        if ("ping".equals(payload)) {
            // 处理心跳消息
            session.sendMessage(new TextMessage("pong"));
        } else if ("status".equals(payload)) {
            // 处理状态查询
            String username = extractUsername(session);
            if (username != null) {
                String statusMessage = String.format("{\"type\":\"status\",\"username\":\"%s\",\"status\":\"online\"}", username);
                session.sendMessage(new TextMessage(statusMessage));
            }
        } else {
            // 其他消息类型
            logger.info("收到未知消息类型: wsId={}, message={}", session.getId(), payload);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("WebSocket连接关闭: wsId={}, status={}", session.getId(), status);
        
        // 通知服务处理连接关闭
        logoutNotificationService.handleWebSocketClose(session, status);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket传输错误: wsId={}, error={}", session.getId(), exception.getMessage(), exception);
        
        // 关闭有问题的连接
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR.withReason("传输错误"));
        }
    }
    
    /**
     * 从WebSocket会话中提取HTTP会话ID
     * 
     * @param session WebSocket会话
     * @return HTTP会话ID
     */
    private String extractHttpSessionId(WebSocketSession session) {
        try {
            // 从WebSocket会话属性中获取HTTP会话
            Object httpSessionObj = session.getAttributes().get("HTTP_SESSION");
            if (httpSessionObj instanceof HttpSession) {
                return ((HttpSession) httpSessionObj).getId();
            }
            
            // 尝试从握手信息中获取
            Object sessionId = session.getHandshakeHeaders().getFirst("X-Session-ID");
            if (sessionId != null) {
                return sessionId.toString();
            }
            
            // 如果都没有，使用WebSocket会话ID作为备选
            return session.getId();
            
        } catch (Exception e) {
            logger.error("提取HTTP会话ID失败: wsId={}, error={}", session.getId(), e.getMessage());
            return null;
        }
    }
    
    /**
     * 从WebSocket会话中提取用户名
     * 
     * @param session WebSocket会话
     * @return 用户名
     */
    private String extractUsername(WebSocketSession session) {
        try {
            // 尝试从安全上下文获取
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
            
            // 尝试从WebSocket会话属性获取
            Object usernameObj = session.getAttributes().get("username");
            if (usernameObj != null) {
                return usernameObj.toString();
            }
            
            // 尝试从握手信息获取
            Object username = session.getHandshakeHeaders().getFirst("X-Username");
            if (username != null) {
                return username.toString();
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("提取用户名失败: wsId={}, error={}", session.getId(), e.getMessage());
            return null;
        }
    }
} 