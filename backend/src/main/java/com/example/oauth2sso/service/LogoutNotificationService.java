package com.example.oauth2sso.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登出通知服务
 * 通过WebSocket实现登出事件的实时通知
 * 
 * @author Luowenqiang
 * @version 1.0.0
 * @since 2024-12-26
 */
@Service
public class LogoutNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogoutNotificationService.class);
    
    // 存储WebSocket会话，key为sessionId，value为WebSocketSession
    private final Map<String, WebSocketSession> webSocketSessions = new ConcurrentHashMap<>();
    
    // 存储用户与WebSocket会话的映射，用于按用户发送通知
    private final Map<String, String> userSessionMap = new ConcurrentHashMap<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 注册WebSocket会话
     * 
     * @param sessionId HTTP会话ID
     * @param username 用户名
     * @param webSocketSession WebSocket会话
     */
    public void registerWebSocketSession(String sessionId, String username, WebSocketSession webSocketSession) {
        logger.info("注册WebSocket会话: sessionId={}, username={}, wsId={}", 
                   sessionId, username, webSocketSession.getId());
        
        webSocketSessions.put(sessionId, webSocketSession);
        userSessionMap.put(username, sessionId);
        
        // 发送连接成功消息
        sendMessage(webSocketSession, new LogoutNotification(
            "CONNECTION_ESTABLISHED", 
            username, 
            "WebSocket连接已建立", 
            Instant.now()
        ));
    }
    
    /**
     * 移除WebSocket会话
     * 
     * @param sessionId HTTP会话ID
     */
    public void removeWebSocketSession(String sessionId) {
        WebSocketSession webSocketSession = webSocketSessions.remove(sessionId);
        if (webSocketSession != null) {
            logger.info("移除WebSocket会话: sessionId={}, wsId={}", sessionId, webSocketSession.getId());
            
            // 从用户映射中移除
            userSessionMap.entrySet().removeIf(entry -> sessionId.equals(entry.getValue()));
        }
    }
    
    /**
     * 发送登出通知
     * 
     * @param targetSessionId 目标会话ID
     * @param username 用户名
     * @param reason 登出原因
     */
    public void sendLogoutNotification(String targetSessionId, String username, String reason) {
        logger.info("发送登出通知: targetSession={}, username={}, reason={}", 
                   targetSessionId, username, reason);
        
        WebSocketSession webSocketSession = webSocketSessions.get(targetSessionId);
        if (webSocketSession != null && webSocketSession.isOpen()) {
            LogoutNotification notification = new LogoutNotification(
                "FORCE_LOGOUT", 
                username, 
                reason, 
                Instant.now()
            );
            
            sendMessage(webSocketSession, notification);
        } else {
            logger.warn("目标WebSocket会话不存在或已关闭: sessionId={}", targetSessionId);
        }
    }
    
    /**
     * 广播登出通知给指定用户的所有会话
     * 
     * @param username 用户名
     * @param reason 登出原因
     */
    public void broadcastLogoutNotification(String username, String reason) {
        logger.info("广播登出通知: username={}, reason={}", username, reason);
        
        LogoutNotification notification = new LogoutNotification(
            "BROADCAST_LOGOUT", 
            username, 
            reason, 
            Instant.now()
        );
        
        // 向所有相关会话发送通知
        webSocketSessions.entrySet().stream()
            .filter(entry -> {
                String sessionId = entry.getKey();
                return userSessionMap.containsValue(sessionId);
            })
            .forEach(entry -> {
                WebSocketSession session = entry.getValue();
                if (session.isOpen()) {
                    sendMessage(session, notification);
                }
            });
    }
    
    /**
     * 发送心跳消息
     * 保持WebSocket连接活跃
     */
    public void sendHeartbeat() {
        logger.debug("发送心跳消息到 {} 个WebSocket会话", webSocketSessions.size());
        
        LogoutNotification heartbeat = new LogoutNotification(
            "HEARTBEAT", 
            "system", 
            "心跳检测", 
            Instant.now()
        );
        
        webSocketSessions.values().stream()
            .filter(WebSocketSession::isOpen)
            .forEach(session -> sendMessage(session, heartbeat));
    }
    
    /**
     * 清理关闭的WebSocket会话
     */
    public void cleanupClosedSessions() {
        int beforeCount = webSocketSessions.size();
        
        webSocketSessions.entrySet().removeIf(entry -> !entry.getValue().isOpen());
        
        int afterCount = webSocketSessions.size();
        int cleanedCount = beforeCount - afterCount;
        
        if (cleanedCount > 0) {
            logger.info("清理了 {} 个已关闭的WebSocket会话", cleanedCount);
        }
    }
    
    /**
     * 处理WebSocket会话关闭
     * 
     * @param webSocketSession WebSocket会话
     * @param closeStatus 关闭状态
     */
    public void handleWebSocketClose(WebSocketSession webSocketSession, CloseStatus closeStatus) {
        logger.info("WebSocket会话关闭: wsId={}, status={}", 
                   webSocketSession.getId(), closeStatus);
        
        // 从映射中移除该会话
        String sessionIdToRemove = null;
        for (Map.Entry<String, WebSocketSession> entry : webSocketSessions.entrySet()) {
            if (entry.getValue().equals(webSocketSession)) {
                sessionIdToRemove = entry.getKey();
                break;
            }
        }
        
        if (sessionIdToRemove != null) {
            removeWebSocketSession(sessionIdToRemove);
        }
    }
    
    /**
     * 发送消息到WebSocket会话
     * 
     * @param webSocketSession WebSocket会话
     * @param notification 通知消息
     */
    private void sendMessage(WebSocketSession webSocketSession, LogoutNotification notification) {
        try {
            if (webSocketSession.isOpen()) {
                String message = objectMapper.writeValueAsString(notification);
                webSocketSession.sendMessage(new TextMessage(message));
                logger.debug("WebSocket消息发送成功: wsId={}, type={}", 
                           webSocketSession.getId(), notification.getType());
            } else {
                logger.warn("WebSocket会话已关闭，无法发送消息: wsId={}", webSocketSession.getId());
            }
        } catch (Exception e) {
            logger.error("WebSocket消息发送失败: wsId={}, error={}", 
                        webSocketSession.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 获取活跃的WebSocket会话数量
     * 
     * @return 活跃会话数量
     */
    public int getActiveWebSocketCount() {
        return (int) webSocketSessions.values().stream()
            .filter(WebSocketSession::isOpen)
            .count();
    }
    
    /**
     * 登出通知消息类
     */
    public static class LogoutNotification {
        private String type;
        private String username;
        private String message;
        private Instant timestamp;
        
        public LogoutNotification() {}
        
        public LogoutNotification(String type, String username, String message, Instant timestamp) {
            this.type = type;
            this.username = username;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        
        @Override
        public String toString() {
            return String.format("LogoutNotification{type='%s', username='%s', message='%s', timestamp=%s}", 
                               type, username, message, timestamp);
        }
    }
} 