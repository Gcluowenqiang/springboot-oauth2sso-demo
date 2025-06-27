package com.example.oauth2sso.controller;

import com.example.oauth2sso.service.OAuth2TokenService;
import com.example.oauth2sso.service.SessionSyncService;
import com.example.oauth2sso.service.LogoutNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 真正的单点登出控制器
 * 实现完整的单点登出功能，包括：
 * 1. 令牌撤销机制
 * 2. 会话同步通知
 * 3. 跨域登出协调
 * 4. WebSocket实时通知
 * 
 * @author Luowenqiang
 * @version 1.0.0
 * @since 2024-12-26
 */
@Controller
@RequestMapping("/sso")
public class SingleSignOutController {
    
    private static final Logger logger = LoggerFactory.getLogger(SingleSignOutController.class);
    
    @Autowired
    private OAuth2TokenService tokenService;
    
    @Autowired
    private SessionSyncService sessionSyncService;
    
    @Autowired
    private LogoutNotificationService notificationService;
    
    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;
    
    /**
     * 显示单点登出确认页面
     * 
     * @param model 模型对象
     * @param authentication 认证信息
     * @return 确认页面
     */
    @GetMapping("/logout")
    public String showLogoutConfirmation(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        String username = authentication.getName();
        int activeSessionCount = sessionSyncService.getActiveSessionCount(username);
        
        model.addAttribute("username", username);
        model.addAttribute("activeSessionCount", activeSessionCount);
        model.addAttribute("logoutTypes", getLogoutTypes());
        
        return "sso-logout-confirm";
    }
    
    /**
     * 执行真正的单点登出
     * 
     * @param logoutType 登出类型 (local, complete, global)
     * @param request HTTP请求
     * @param response HTTP响应
     * @param authentication 认证信息
     * @return 登出结果页面
     */
    @PostMapping("/logout")
    public String performSingleSignOut(@RequestParam(defaultValue = "complete") String logoutType,
                                     HttpServletRequest request,
                                     HttpServletResponse response,
                                     Authentication authentication,
                                     Model model) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        String username = authentication.getName();
        String currentSessionId = request.getSession().getId();
        
        logger.info("开始执行单点登出: username={}, sessionId={}, type={}", 
                   username, currentSessionId, logoutType);
        
        SessionSyncService.LogoutResult result;
        
        try {
            switch (logoutType) {
                case "local":
                    result = performLocalLogout(request, response, authentication);
                    break;
                case "complete":
                    result = performCompleteLogout(request, response, authentication, currentSessionId);
                    break;
                case "global":
                    result = performGlobalLogout(request, response, authentication, currentSessionId);
                    break;
                default:
                    result = performCompleteLogout(request, response, authentication, currentSessionId);
            }
            
            // 添加结果到模型
            model.addAttribute("logoutResult", result);
            model.addAttribute("logoutType", logoutType);
            
            return "sso-logout-result";
            
        } catch (Exception e) {
            logger.error("单点登出执行失败: username={}, error={}", username, e.getMessage(), e);
            model.addAttribute("error", "登出过程中发生错误: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * API接口：执行单点登出
     * 
     * @param logoutType 登出类型
     * @param request HTTP请求
     * @param response HTTP响应
     * @param authentication 认证信息
     * @return JSON响应
     */
    @PostMapping("/api/logout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiSingleSignOut(
            @RequestParam(defaultValue = "complete") String logoutType,
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        
        Map<String, Object> responseData = new HashMap<>();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            responseData.put("success", false);
            responseData.put("message", "用户未认证");
            return ResponseEntity.status(401).body(responseData);
        }
        
        String username = authentication.getName();
        String currentSessionId = request.getSession().getId();
        
        try {
            SessionSyncService.LogoutResult result;
            
            switch (logoutType) {
                case "local":
                    result = performLocalLogout(request, response, authentication);
                    break;
                case "complete":
                    result = performCompleteLogout(request, response, authentication, currentSessionId);
                    break;
                case "global":
                    result = performGlobalLogout(request, response, authentication, currentSessionId);
                    break;
                default:
                    result = performCompleteLogout(request, response, authentication, currentSessionId);
            }
            
            responseData.put("success", result.isSuccess());
            responseData.put("message", result.getMessage());
            responseData.put("expiredSessions", result.getExpiredSessions().size());
            responseData.put("tokenRevoked", result.isTokenRevoked());
            
            return ResponseEntity.ok(responseData);
            
        } catch (Exception e) {
            logger.error("API单点登出失败: username={}, error={}", username, e.getMessage(), e);
            responseData.put("success", false);
            responseData.put("message", "登出失败: " + e.getMessage());
            return ResponseEntity.status(500).body(responseData);
        }
    }
    
    /**
     * 获取会话状态信息
     * 
     * @param authentication 认证信息
     * @return JSON响应
     */
    @GetMapping("/api/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSessionStatus(Authentication authentication) {
        Map<String, Object> statusData = new HashMap<>();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            statusData.put("authenticated", false);
            return ResponseEntity.ok(statusData);
        }
        
        String username = authentication.getName();
        int activeSessionCount = sessionSyncService.getActiveSessionCount(username);
        int activeWebSocketCount = notificationService.getActiveWebSocketCount();
        
        statusData.put("authenticated", true);
        statusData.put("username", username);
        statusData.put("activeSessionCount", activeSessionCount);
        statusData.put("activeWebSocketCount", activeWebSocketCount);
        
        return ResponseEntity.ok(statusData);
    }
    
    /**
     * 执行本地登出
     * 仅清除当前会话，不影响其他会话和令牌
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @param authentication 认证信息
     * @return 登出结果
     */
    private SessionSyncService.LogoutResult performLocalLogout(HttpServletRequest request,
                                                             HttpServletResponse response,
                                                             Authentication authentication) {
        String username = authentication.getName();
        logger.info("执行本地登出: username={}", username);
        
        // 执行Spring Security标准登出
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        
        // 创建结果对象
        SessionSyncService.LogoutResult result = new SessionSyncService.LogoutResult();
        result.setUsername(username);
        result.setSuccess(true);
        result.setMessage("本地登出成功");
        result.setTokenRevoked(false);
        
        return result;
    }
    
    /**
     * 执行完整登出
     * 清除用户的所有会话，但不撤销OAuth2令牌
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @param authentication 认证信息
     * @param currentSessionId 当前会话ID
     * @return 登出结果
     */
    private SessionSyncService.LogoutResult performCompleteLogout(HttpServletRequest request,
                                                                HttpServletResponse response,
                                                                Authentication authentication,
                                                                String currentSessionId) {
        String username = authentication.getName();
        logger.info("执行完整登出: username={}", username);
        
        // 执行单点登出（清除所有会话）
        SessionSyncService.LogoutResult result = sessionSyncService.performSingleSignOut(username, currentSessionId);
        
        // 执行当前会话的标准登出
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        
        // 广播登出通知
        notificationService.broadcastLogoutNotification(username, "完整登出");
        
        result.setMessage("完整登出成功，已清除所有会话");
        return result;
    }
    
    /**
     * 执行全局登出
     * 清除所有会话并撤销OAuth2令牌
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @param authentication 认证信息
     * @param currentSessionId 当前会话ID
     * @return 登出结果
     */
    private SessionSyncService.LogoutResult performGlobalLogout(HttpServletRequest request,
                                                              HttpServletResponse response,
                                                              Authentication authentication,
                                                              String currentSessionId) {
        String username = authentication.getName();
        logger.info("执行全局登出: username={}", username);
        
        // 获取OAuth2访问令牌
        String accessToken = extractAccessToken(authentication);
        
        // 执行单点登出（清除所有会话）
        SessionSyncService.LogoutResult result = sessionSyncService.performSingleSignOut(username, currentSessionId);
        
        // 撤销OAuth2令牌
        boolean tokenRevoked = false;
        if (accessToken != null) {
            logger.info("开始撤销OAuth2令牌: token={}***", 
                       accessToken.substring(0, Math.min(8, accessToken.length())));
            
            // 首先尝试主要的撤销方法
            tokenRevoked = tokenService.revokeGitHubToken(accessToken);
            
            if (!tokenRevoked) {
                logger.warn("主要撤销方法失败，尝试备用方法");
                // 如果主要方法失败，尝试备用方法
                tokenRevoked = tokenService.revokeTokenDirectly(accessToken);
            }
            
            if (!tokenRevoked) {
                logger.warn("令牌撤销失败，但会话已清除。用户可能需要手动撤销GitHub授权");
                result.setMessage("全局登出部分成功：已清除所有会话，但令牌撤销失败。" +
                                "建议访问 https://github.com/settings/applications 手动撤销授权");
            } else {
                logger.info("令牌撤销成功");
            }
        } else {
            logger.warn("无法获取访问令牌，跳过令牌撤销");
            result.setMessage("全局登出完成：已清除所有会话，但无法获取令牌进行撤销");
        }
        
        // 执行当前会话的标准登出
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        
        // 广播登出通知
        String notificationMessage = tokenRevoked ? "全局登出，令牌已撤销" : "全局登出，会话已清除";
        notificationService.broadcastLogoutNotification(username, notificationMessage);
        
        result.setTokenRevoked(tokenRevoked);
        if (tokenRevoked) {
            result.setMessage("全局登出成功，已清除所有会话并撤销令牌");
        }
        
        return result;
    }
    
    /**
     * 从认证对象中提取OAuth2访问令牌
     * 通过OAuth2AuthorizedClientService获取授权客户端信息
     * 
     * @param authentication 认证对象
     * @return 访问令牌
     */
    private String extractAccessToken(Authentication authentication) {
        try {
            if (authentication instanceof OAuth2AuthenticationToken) {
                OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
                String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
                String principalName = oauth2Token.getName();
                
                logger.debug("尝试获取访问令牌: registrationId={}, principalName={}", registrationId, principalName);
                
                // 通过OAuth2AuthorizedClientService获取授权客户端
                OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                    registrationId, principalName);
                
                if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                    String accessToken = authorizedClient.getAccessToken().getTokenValue();
                    logger.info("成功获取访问令牌: token={}***", accessToken.substring(0, Math.min(8, accessToken.length())));
                    return accessToken;
                } else {
                    logger.warn("无法获取授权客户端或访问令牌: registrationId={}, principalName={}", registrationId, principalName);
                }
            } else {
                logger.warn("认证对象不是OAuth2AuthenticationToken类型: {}", authentication.getClass());
            }
            return null;
        } catch (Exception e) {
            logger.error("提取访问令牌失败: error={}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 获取登出类型选项
     * 
     * @return 登出类型映射
     */
    private Map<String, String> getLogoutTypes() {
        Map<String, String> types = new HashMap<>();
        types.put("local", "仅本地登出");
        types.put("complete", "完整登出（清除所有会话）");
        types.put("global", "全局登出（撤销令牌）");
        return types;
    }
} 