package com.example.oauth2sso.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话同步服务
 * 实现会话管理、同步通知和跨域登出协调
 * 
 * @author Luowenqiang
 * @version 1.0.0
 * @since 2024-12-26
 */
@Service
public class SessionSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionSyncService.class);
    
    @Autowired
    @Lazy
    private SessionRegistry sessionRegistry;
    
    @Autowired
    private LogoutNotificationService logoutNotificationService;
    
    @Autowired
    private OAuth2TokenService oauth2TokenService;
    
    // 用户会话映射表 - 用于跟踪用户的所有会话
    private final Map<String, Set<String>> userSessionMap = new ConcurrentHashMap<>();
    
    // 会话元数据存储
    private final Map<String, SessionMetadata> sessionMetadataMap = new ConcurrentHashMap<>();
    
    /**
     * 注册用户会话
     * 
     * @param username 用户名
     * @param sessionId 会话ID
     * @param accessToken OAuth2访问令牌
     */
    public void registerUserSession(String username, String sessionId, String accessToken) {
        logger.info("🔄 注册用户会话: username={}, sessionId={}, hasToken={}", 
                   username, sessionId, accessToken != null);
        
        // 添加到用户会话映射
        userSessionMap.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        
        // 存储会话元数据
        SessionMetadata metadata = new SessionMetadata(username, sessionId, accessToken, Instant.now());
        sessionMetadataMap.put(sessionId, metadata);
        
        int currentSessionCount = userSessionMap.get(username).size();
        logger.info("✅ 会话注册成功: 用户 {} 当前有 {} 个活跃会话", username, currentSessionCount);
        
        // 调试：显示所有已注册的会话
        logger.debug("📊 当前所有用户会话: {}", userSessionMap.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                java.util.Map.Entry::getKey, 
                entry -> entry.getValue().size()
            )));
    }
    
    /**
     * 执行单点登出
     * 清除用户的所有会话，实现真正的单点登出
     * 
     * @param username 用户名
     * @param currentSessionId 当前会话ID（可选，用于排除当前会话）
     * @return 登出结果
     */
    public LogoutResult performSingleSignOut(String username, String currentSessionId) {
        logger.info("🚪 执行单点登出: username={}, currentSession={}", username, currentSessionId);
        
        LogoutResult result = new LogoutResult();
        result.setUsername(username);
        result.setStartTime(Instant.now());
        
        Set<String> userSessions = userSessionMap.get(username);
        if (userSessions == null || userSessions.isEmpty()) {
            logger.warn("⚠️  用户 {} 没有找到活跃会话，可能会话未正确注册", username);
            logger.debug("📊 当前用户会话映射状态: {}", userSessionMap.keySet());
            result.setSuccess(true);
            result.setMessage("没有找到活跃会话（可能会话未正确注册）");
            return result;
        }
        
        logger.info("📋 找到用户 {} 的 {} 个活跃会话: {}", username, userSessions.size(), userSessions);
        
        List<String> expiredSessions = new ArrayList<>();
        List<String> failedSessions = new ArrayList<>();
        String accessToken = null;
        
        // 遍历用户的所有会话
        for (String sessionId : new HashSet<>(userSessions)) {
            if (sessionId.equals(currentSessionId)) {
                continue; // 跳过当前会话，最后处理
            }
            
            try {
                // 获取会话元数据
                SessionMetadata metadata = sessionMetadataMap.get(sessionId);
                if (metadata != null && accessToken == null) {
                    accessToken = metadata.getAccessToken();
                }
                
                // 使会话失效
                expireSession(sessionId);
                expiredSessions.add(sessionId);
                
                // 发送实时通知
                logoutNotificationService.sendLogoutNotification(sessionId, username, "单点登出");
                
            } catch (Exception e) {
                logger.error("会话失效失败: sessionId={}, error={}", sessionId, e.getMessage());
                failedSessions.add(sessionId);
            }
        }
        
        // 清理用户会话映射
        userSessionMap.remove(username);
        
        // 撤销OAuth2令牌（如果有）
        boolean tokenRevoked = false;
        if (accessToken != null) {
            tokenRevoked = revokeUserToken(accessToken);
        }
        
        result.setExpiredSessions(expiredSessions);
        result.setFailedSessions(failedSessions);
        result.setTokenRevoked(tokenRevoked);
        result.setEndTime(Instant.now());
        result.setSuccess(failedSessions.isEmpty());
        result.setMessage(String.format("成功登出 %d 个会话，失败 %d 个会话", 
                         expiredSessions.size(), failedSessions.size()));
        
        logger.info("单点登出完成: {}", result);
        return result;
    }
    
    /**
     * 使指定会话失效
     * 
     * @param sessionId 会话ID
     */
    private void expireSession(String sessionId) {
        try {
            // 通过SessionRegistry使会话失效
            List<Object> principals = sessionRegistry.getAllPrincipals();
            for (Object principal : principals) {
                List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                for (SessionInformation session : sessions) {
                    if (sessionId.equals(session.getSessionId())) {
                        session.expireNow();
                        logger.debug("会话已失效: sessionId={}", sessionId);
                        break;
                    }
                }
            }
            
            // 清理会话元数据
            sessionMetadataMap.remove(sessionId);
            
        } catch (Exception e) {
            logger.error("使会话失效时发生错误: sessionId={}, error={}", sessionId, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 撤销用户令牌
     * 
     * @param accessToken 访问令牌
     * @return 撤销是否成功
     */
    private boolean revokeUserToken(String accessToken) {
        try {
            if (accessToken == null || accessToken.isEmpty()) {
                logger.warn("访问令牌为空，跳过令牌撤销");
                return false;
            }
            
            logger.info("开始撤销用户令牌");
            boolean result = oauth2TokenService.revokeGitHubToken(accessToken);
            
            if (result) {
                logger.info("用户令牌撤销成功");
            } else {
                logger.warn("用户令牌撤销失败");
            }
            
            return result;
        } catch (Exception e) {
            logger.error("撤销令牌时发生异常: error={}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 清理过期会话
     * 定期清理已过期的会话信息
     */
    public void cleanupExpiredSessions() {
        logger.debug("开始清理过期会话");
        
        Iterator<Map.Entry<String, SessionMetadata>> iterator = sessionMetadataMap.entrySet().iterator();
        int cleanedCount = 0;
        
        while (iterator.hasNext()) {
            Map.Entry<String, SessionMetadata> entry = iterator.next();
            String sessionId = entry.getKey();
            SessionMetadata metadata = entry.getValue();
            
            // 检查会话是否仍然有效
            if (isSessionExpired(sessionId)) {
                iterator.remove();
                
                // 从用户会话映射中移除
                Set<String> userSessions = userSessionMap.get(metadata.getUsername());
                if (userSessions != null) {
                    userSessions.remove(sessionId);
                    if (userSessions.isEmpty()) {
                        userSessionMap.remove(metadata.getUsername());
                    }
                }
                
                cleanedCount++;
            }
        }
        
        if (cleanedCount > 0) {
            logger.info("清理了 {} 个过期会话", cleanedCount);
        }
    }
    
    /**
     * 检查会话是否已过期
     * 
     * @param sessionId 会话ID
     * @return 是否过期
     */
    private boolean isSessionExpired(String sessionId) {
        try {
            List<Object> principals = sessionRegistry.getAllPrincipals();
            for (Object principal : principals) {
                List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                for (SessionInformation session : sessions) {
                    if (sessionId.equals(session.getSessionId())) {
                        return session.isExpired();
                    }
                }
            }
            return true; // 如果找不到会话，认为已过期
        } catch (Exception e) {
            logger.error("检查会话状态失败: sessionId={}, error={}", sessionId, e.getMessage());
            return true; // 出错时认为已过期
        }
    }
    
    /**
     * 获取用户的活跃会话数量
     * 
     * @param username 用户名
     * @return 活跃会话数量
     */
    public int getActiveSessionCount(String username) {
        Set<String> sessions = userSessionMap.get(username);
        return sessions != null ? sessions.size() : 0;
    }
    
    /**
     * 会话元数据类
     */
    public static class SessionMetadata {
        private final String username;
        private final String sessionId;
        private final String accessToken;
        private final Instant createdTime;
        
        public SessionMetadata(String username, String sessionId, String accessToken, Instant createdTime) {
            this.username = username;
            this.sessionId = sessionId;
            this.accessToken = accessToken;
            this.createdTime = createdTime;
        }
        
        // Getters
        public String getUsername() { return username; }
        public String getSessionId() { return sessionId; }
        public String getAccessToken() { return accessToken; }
        public Instant getCreatedTime() { return createdTime; }
    }
    
    /**
     * 登出结果类
     */
    public static class LogoutResult {
        private String username;
        private List<String> expiredSessions = new ArrayList<>();
        private List<String> failedSessions = new ArrayList<>();
        private boolean tokenRevoked;
        private boolean success;
        private String message;
        private Instant startTime;
        private Instant endTime;
        
        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public List<String> getExpiredSessions() { return expiredSessions; }
        public void setExpiredSessions(List<String> expiredSessions) { this.expiredSessions = expiredSessions; }
        
        public List<String> getFailedSessions() { return failedSessions; }
        public void setFailedSessions(List<String> failedSessions) { this.failedSessions = failedSessions; }
        
        public boolean isTokenRevoked() { return tokenRevoked; }
        public void setTokenRevoked(boolean tokenRevoked) { this.tokenRevoked = tokenRevoked; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Instant getStartTime() { return startTime; }
        public void setStartTime(Instant startTime) { this.startTime = startTime; }
        
        public Instant getEndTime() { return endTime; }
        public void setEndTime(Instant endTime) { this.endTime = endTime; }
        
        @Override
        public String toString() {
            return String.format("LogoutResult{username='%s', success=%s, expiredSessions=%d, failedSessions=%d, tokenRevoked=%s, message='%s'}", 
                               username, success, expiredSessions.size(), failedSessions.size(), tokenRevoked, message);
        }
    }
} 