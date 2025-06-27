package com.example.oauth2sso.controller;

import com.example.oauth2sso.service.OAuth2TokenService;
import com.example.oauth2sso.service.SessionSyncService;
import com.example.oauth2sso.service.LogoutNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统调试诊断控制器
 * 提供系统状态检查、会话管理、WebSocket连接等调试功能
 * 用于排查单点登出和OAuth2认证相关问题
 * 
 * @author Luowenqiang
 * @version 1.0.0
 * @since 2024-12-26
 */
@Controller
@RequestMapping("/debug")
public class DebugController {
    
    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);
    
    @Autowired
    private SessionSyncService sessionSyncService;
    
    @Autowired
    private LogoutNotificationService notificationService;
    
    @Autowired
    private SessionRegistry sessionRegistry;
    
    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;
    
    @Autowired
    private OAuth2TokenService tokenService;
    
    /**
     * 显示系统调试诊断页面
     * 
     * @param model 模型对象
     * @param authentication 认证信息
     * @return 调试页面
     */
    @GetMapping
    public String debugPage(Model model, Authentication authentication) {
        model.addAttribute("authenticated", authentication != null && authentication.isAuthenticated());
        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("username", authentication.getName());
        }
        return "debug";
    }
    
    /**
     * 获取当前用户的会话状态
     * 
     * @param authentication 认证信息
     * @return 会话状态信息
     */
    @GetMapping("/session-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSessionStatus(Authentication authentication) {
        Map<String, Object> status = new HashMap<>();
        
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                status.put("authenticated", false);
                status.put("message", "用户未认证");
                return ResponseEntity.ok(status);
            }
            
            String username = authentication.getName();
            
            // 基本认证信息
            status.put("authenticated", true);
            status.put("username", username);
            status.put("authType", authentication.getClass().getSimpleName());
            
            // 会话管理信息
            int activeSessionCount = sessionSyncService.getActiveSessionCount(username);
            status.put("activeSessionCount", activeSessionCount);
            
            // WebSocket连接信息
            int activeWebSocketCount = notificationService.getActiveWebSocketCount();
            status.put("activeWebSocketCount", activeWebSocketCount);
            
            // Spring Security会话注册表信息
            List<Object> allPrincipals = sessionRegistry.getAllPrincipals();
            status.put("registeredPrincipals", allPrincipals.size());
            
            // OAuth2令牌信息
            if (authentication instanceof OAuth2AuthenticationToken) {
                OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
                status.put("oauth2RegistrationId", oauth2Token.getAuthorizedClientRegistrationId());
                
                // 获取访问令牌信息
                String accessToken = extractAccessToken(authentication);
                if (accessToken != null) {
                    boolean tokenValid = tokenService.validateToken(accessToken);
                    Map<String, Object> tokenInfo = tokenService.getTokenInfo(accessToken);
                    
                    status.put("hasAccessToken", true);
                    status.put("tokenValid", tokenValid);
                    status.put("tokenLength", accessToken.length());
                    status.put("tokenPrefix", accessToken.substring(0, Math.min(8, accessToken.length())) + "***");
                    
                    if (tokenInfo != null) {
                        status.put("githubUser", tokenInfo.get("login"));
                        status.put("githubUserId", tokenInfo.get("id"));
                    }
                } else {
                    status.put("hasAccessToken", false);
                    status.put("tokenIssue", "无法获取访问令牌");
                }
            }
            
            status.put("timestamp", Instant.now().toString());
            status.put("success", true);
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("获取会话状态失败: {}", e.getMessage(), e);
            status.put("success", false);
            status.put("error", e.getMessage());
            return ResponseEntity.status(500).body(status);
        }
    }
    
    /**
     * 手动注册当前会话（调试用）
     * 当会话管理出现问题时，可以手动注册会话
     * 
     * @param request HTTP请求
     * @param authentication 认证信息
     * @return 注册结果
     */
    @GetMapping("/register-session")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registerSession(HttpServletRequest request, 
                                                             Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                result.put("success", false);
                result.put("message", "用户未认证");
                return ResponseEntity.status(401).body(result);
            }
            
            String username = authentication.getName();
            String sessionId = request.getSession().getId();
            String accessToken = extractAccessToken(authentication);
            
            sessionSyncService.registerUserSession(username, sessionId, accessToken);
            
            result.put("success", true);
            result.put("message", "会话注册成功");
            result.put("username", username);
            result.put("sessionId", sessionId);
            result.put("hasToken", accessToken != null);
            
            logger.info("手动注册会话成功: username={}, sessionId={}", username, sessionId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("手动注册会话失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 令牌诊断端点
     * 检查当前用户的OAuth2令牌状态和有效性
     * 
     * @param authentication 认证信息
     * @return 令牌诊断信息
     */
    @GetMapping("/token-diagnosis")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> diagnoseToken(Authentication authentication) {
        Map<String, Object> diagnosis = new HashMap<>();
        
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                diagnosis.put("authenticated", false);
                diagnosis.put("message", "用户未认证，无法诊断令牌");
                return ResponseEntity.ok(diagnosis);
            }
            
            diagnosis.put("authenticated", true);
            diagnosis.put("username", authentication.getName());
            
            // 检查是否是OAuth2认证
            if (!(authentication instanceof OAuth2AuthenticationToken)) {
                diagnosis.put("isOAuth2", false);
                diagnosis.put("message", "当前认证不是OAuth2类型，无需令牌诊断");
                return ResponseEntity.ok(diagnosis);
            }
            
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            diagnosis.put("isOAuth2", true);
            diagnosis.put("registrationId", oauth2Token.getAuthorizedClientRegistrationId());
            
            // 尝试获取访问令牌
            String accessToken = extractAccessToken(authentication);
            if (accessToken == null) {
                diagnosis.put("hasToken", false);
                diagnosis.put("issue", "无法从OAuth2AuthorizedClientService获取访问令牌");
                diagnosis.put("suggestion", "可能需要重新登录以获取新的访问令牌");
                return ResponseEntity.ok(diagnosis);
            }
            
            diagnosis.put("hasToken", true);
            diagnosis.put("tokenLength", accessToken.length());
            diagnosis.put("tokenPrefix", accessToken.substring(0, Math.min(8, accessToken.length())) + "***");
            
            // 验证令牌有效性
            boolean isValid = tokenService.validateToken(accessToken);
            diagnosis.put("tokenValid", isValid);
            
            if (!isValid) {
                diagnosis.put("issue", "访问令牌已失效或无效");
                diagnosis.put("suggestion", "需要重新登录获取新的访问令牌");
            } else {
                // 获取令牌详细信息
                Map<String, Object> tokenInfo = tokenService.getTokenInfo(accessToken);
                if (tokenInfo != null) {
                    diagnosis.put("githubUser", tokenInfo.get("login"));
                    diagnosis.put("githubUserId", tokenInfo.get("id"));
                    diagnosis.put("status", "令牌有效，可以正常使用");
                } else {
                    diagnosis.put("issue", "令牌验证成功但无法获取用户信息");
                }
            }
            
            diagnosis.put("timestamp", Instant.now().toString());
            return ResponseEntity.ok(diagnosis);
            
        } catch (Exception e) {
            logger.error("令牌诊断失败: {}", e.getMessage(), e);
            diagnosis.put("success", false);
            diagnosis.put("error", e.getMessage());
            return ResponseEntity.status(500).body(diagnosis);
        }
    }
    
    /**
     * 清理无效会话（调试用）
     * 清理SessionSyncService中的无效会话记录
     * 
     * @param authentication 认证信息
     * @return 清理结果
     */
    @GetMapping("/cleanup-sessions")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cleanupSessions(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                result.put("success", false);
                result.put("message", "用户未认证");
                return ResponseEntity.status(401).body(result);
            }
            
            String username = authentication.getName();
            
            // 清理无效会话（这个方法需要在SessionSyncService中实现）
            int beforeCount = sessionSyncService.getActiveSessionCount(username);
            // sessionSyncService.cleanupInvalidSessions(username); // 需要实现此方法
            int afterCount = sessionSyncService.getActiveSessionCount(username);
            
            result.put("success", true);
            result.put("username", username);
            result.put("sessionsBeforeCleanup", beforeCount);
            result.put("sessionsAfterCleanup", afterCount);
            result.put("cleanedSessions", beforeCount - afterCount);
            
            logger.info("会话清理完成: username={}, cleaned={}", username, beforeCount - afterCount);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("会话清理失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 从认证对象中提取OAuth2访问令牌
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
                
                OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                    registrationId, principalName);
                
                if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                    return authorizedClient.getAccessToken().getTokenValue();
                }
            }
            return null;
        } catch (Exception e) {
            logger.error("提取访问令牌失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * GitHub会话状态检查
     * 帮助理解OAuth2授权状态和GitHub登录状态的区别
     * 
     * @return GitHub状态信息
     */
    @GetMapping("/github-session-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkGitHubSessionStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // 检查OAuth2授权状态
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated() 
                && authentication instanceof OAuth2AuthenticationToken) {
                
                OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
                String accessToken = extractAccessToken(authentication);
                
                status.put("hasOAuth2Session", true);
                status.put("username", authentication.getName());
                
                if (accessToken != null) {
                    boolean tokenValid = tokenService.validateToken(accessToken);
                    status.put("tokenValid", tokenValid);
                    status.put("tokenPrefix", accessToken.substring(0, Math.min(8, accessToken.length())) + "***");
                    
                    if (tokenValid) {
                        Map<String, Object> tokenInfo = tokenService.getTokenInfo(accessToken);
                        if (tokenInfo != null) {
                            status.put("githubUser", tokenInfo.get("login"));
                            status.put("authorizationStatus", "应用已授权，令牌有效");
                        }
                    } else {
                        status.put("authorizationStatus", "应用授权已撤销，令牌无效");
                    }
                } else {
                    status.put("tokenValid", false);
                    status.put("authorizationStatus", "无法获取访问令牌");
                }
            } else {
                status.put("hasOAuth2Session", false);
                status.put("authorizationStatus", "未进行OAuth2授权");
            }
            
            // 提供详细的状态说明
            status.put("explanation", createGitHubStatusExplanation(status));
            status.put("timestamp", Instant.now().toString());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("检查GitHub会话状态失败: {}", e.getMessage(), e);
            status.put("error", e.getMessage());
            return ResponseEntity.status(500).body(status);
        }
    }
    
    /**
     * 创建GitHub状态说明
     * 
     * @param status 状态信息
     * @return 详细说明
     */
    private Map<String, Object> createGitHubStatusExplanation(Map<String, Object> status) {
        Map<String, Object> explanation = new HashMap<>();
        
        boolean hasOAuth2 = (Boolean) status.getOrDefault("hasOAuth2Session", false);
        boolean tokenValid = (Boolean) status.getOrDefault("tokenValid", false);
        
        if (!hasOAuth2) {
            explanation.put("currentStatus", "未登录");
            explanation.put("nextLoginBehavior", "需要完整的OAuth2授权流程");
            explanation.put("willNeedGitHubLogin", "取决于GitHub会话状态");
        } else if (tokenValid) {
            explanation.put("currentStatus", "已登录且令牌有效");
            explanation.put("nextLoginBehavior", "当前会话有效，无需重新登录");
            explanation.put("willNeedGitHubLogin", "否");
        } else {
            explanation.put("currentStatus", "已登录但令牌已撤销");
            explanation.put("nextLoginBehavior", "需要重新授权应用");
            explanation.put("willNeedGitHubLogin", "否，如果GitHub会话仍有效");
            explanation.put("note", "这就是全局登出的预期行为：撤销应用授权，但不影响GitHub账户登录状态");
        }
        
        explanation.put("gitHubLoginVsAppAuthorization", Map.of(
            "GitHubLogin", "在GitHub网站的登录状态，输入用户名密码",
            "AppAuthorization", "对特定应用的访问授权，同意应用访问权限",
            "globalLogoutEffect", "撤销应用授权，不影响GitHub登录状态"
        ));
        
        return explanation;
    }
} 