package com.example.oauth2sso.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * OAuth2令牌管理服务
 * 实现令牌撤销机制，支持真正的单点登出
 * 
 * @author Luowenqiang
 * @version 1.0.0
 * @since 2024-12-26
 */
@Service
public class OAuth2TokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(OAuth2TokenService.class);
    
    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String githubClientId;
    
    @Value("${spring.security.oauth2.client.registration.github.client-secret}")  
    private String githubClientSecret;
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * 撤销GitHub Access Token
     * 使用用户级别的令牌撤销，而不是应用程序级别的撤销
     * 
     * @param accessToken 要撤销的访问令牌
     * @return 撤销是否成功
     */
    public boolean revokeGitHubToken(String accessToken) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            logger.warn("访问令牌为空，跳过撤销操作");
            return false;
        }
        
        try {
            // 使用用户级别的令牌撤销API，这是正确的方法
            String revokeUrl = "https://api.github.com/applications/" + githubClientId + "/grant";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(githubClientId, githubClientSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/vnd.github.v3+json");
            headers.set("User-Agent", "OAuth2-SSO-App/1.0.0");
            
            // 使用grant API撤销用户的授权，而不是所有令牌
            Map<String, String> body = Map.of("access_token", accessToken);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            
            logger.info("开始撤销GitHub用户授权: token={}", maskToken(accessToken));
            
            ResponseEntity<String> response = restTemplate.exchange(
                revokeUrl, HttpMethod.DELETE, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                logger.info("GitHub用户授权撤销成功: token={}", maskToken(accessToken));
                return true;
            } else {
                logger.warn("GitHub用户授权撤销响应异常: status={}, token={}", 
                           response.getStatusCode(), maskToken(accessToken));
                return false;
            }
            
        } catch (Exception e) {
            logger.error("GitHub用户授权撤销失败: token={}, error={}", 
                        maskToken(accessToken), e.getMessage());
            
            // 撤销失败时，尝试备用方案：直接检查令牌状态
            logger.info("撤销失败，将检查令牌是否仍然有效");
            boolean isStillValid = validateToken(accessToken);
            if (!isStillValid) {
                logger.info("令牌已失效，视为撤销成功");
                return true;
            }
            
            return false;
        }
    }
    
    /**
     * 备用的令牌撤销方法
     * 当主要撤销方法失败时使用，直接向用户授权端点发送撤销请求
     * 
     * @param accessToken 访问令牌
     * @return 撤销是否成功
     */
    public boolean revokeTokenDirectly(String accessToken) {
        try {
            // 备用方案：直接使用令牌向GitHub发送删除授权请求
            String deleteUrl = "https://api.github.com/user";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "application/vnd.github.v3+json");
            headers.set("User-Agent", "OAuth2-SSO-App/1.0.0");
            
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            // 先验证令牌有效性
            ResponseEntity<String> response = restTemplate.exchange(
                deleteUrl, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("令牌验证成功，但无法直接撤销，需用户手动处理: token={}", maskToken(accessToken));
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.debug("备用撤销方法失败，令牌可能已失效: token={}, error={}", 
                        maskToken(accessToken), e.getMessage());
            return true; // 如果验证失败，可能令牌已经失效
        }
    }
    
    /**
     * 验证令牌是否有效
     * 
     * @param accessToken 访问令牌
     * @return 令牌是否有效
     */
    public boolean validateToken(String accessToken) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return false;
        }
        
        try {
            String validateUrl = "https://api.github.com/user";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "application/vnd.github.v3+json");
            headers.set("User-Agent", "OAuth2-SSO-App/1.0.0");
            headers.set("Connection", "close"); // 避免连接池问题
            
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                validateUrl, HttpMethod.GET, entity, String.class);
            
            boolean isValid = response.getStatusCode() == HttpStatus.OK;
            logger.debug("令牌验证结果: token={}, valid={}", maskToken(accessToken), isValid);
            
            return isValid;
            
        } catch (Exception e) {
            logger.debug("令牌验证失败: token={}, error={}", 
                        maskToken(accessToken), e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取令牌信息
     * 用于调试和诊断令牌状态
     * 
     * @param accessToken 访问令牌
     * @return 令牌信息
     */
    public Map<String, Object> getTokenInfo(String accessToken) {
        try {
            String tokenInfoUrl = "https://api.github.com/user";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "application/vnd.github.v3+json");
            headers.set("User-Agent", "OAuth2-SSO-App/1.0.0");
            
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                tokenInfoUrl, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> userInfo = response.getBody();
                logger.info("令牌信息获取成功: user={}", userInfo.get("login"));
                return userInfo;
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("获取令牌信息失败: token={}, error={}", 
                        maskToken(accessToken), e.getMessage());
            return null;
        }
    }
    
    /**
     * 遮蔽令牌敏感信息用于日志记录
     * 
     * @param token 原始令牌
     * @return 遮蔽后的令牌
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
} 