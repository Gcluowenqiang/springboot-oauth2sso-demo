package com.example.oauth2sso.config;

import com.example.oauth2sso.service.CustomOAuth2UserService;
import com.example.oauth2sso.service.SessionSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Spring Security配置类
 * 配置OAuth2客户端和安全策略
 * 
 * @author Luowenqiang
 * @version 1.0.0
 * @since 2024-12-26
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    
    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;
    
    @Autowired
    @Lazy
    private SessionSyncService sessionSyncService;
    
    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;
    
    /**
     * 配置安全过滤器链
     * 
     * @param http HttpSecurity对象
     * @return SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF，因为我们使用的是OAuth2
            .csrf(csrf -> csrf.disable())
            
            // 配置请求授权
            .authorizeHttpRequests(authz -> authz
                // 公开访问的路径
                .requestMatchers(
                    "/", 
                    "/login", 
                    "/oauth2/**", 
                    "/error",
                    "/css/**", 
                    "/js/**", 
                    "/images/**",
                    "/favicon.ico",
                    "/actuator/health",
                    "/ws/**"  // WebSocket端点
                ).permitAll()
                
                // API接口需要认证
                .requestMatchers("/api/**").authenticated()
                
                // 用户相关页面需要认证
                .requestMatchers("/profile", "/user/**").authenticated()
                
                // 单点登出功能需要认证
                .requestMatchers("/sso/**").authenticated()
                
                // 调试端点需要认证
                .requestMatchers("/debug/**").authenticated()
                
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )
            
            // 配置OAuth2登录
            .oauth2Login(oauth2 -> oauth2
                // 自定义登录页面路径
                .loginPage("/login")
                
                // 设置登录成功后的重定向处理
                .successHandler(authenticationSuccessHandler())
                
                // 设置登录失败后的处理
                .failureHandler((request, response, exception) -> {
                    logger.error("OAuth2登录失败: {}", exception.getMessage(), exception);
                    
                    // 清除可能存在的无效会话
                    request.getSession().invalidate();
                    
                    // 重定向到登录页面并显示错误信息
                    response.sendRedirect("/login?error=oauth2_failed");
                })
                
                // 使用自定义的OAuth2用户服务
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
            )
            
            // 配置登出
            .logout(logout -> logout
                // 登出URL
                .logoutUrl("/logout")
                
                // 登出成功后重定向到单点登出页面
                .logoutSuccessUrl("/sso/logout")
                
                // 清除认证信息
                .clearAuthentication(true)
                
                // 无效化会话
                .invalidateHttpSession(true)
                
                // 删除cookies
                .deleteCookies("JSESSIONID")
            )
            
            // 配置会话管理
            .sessionManagement(session -> session
                // 设置会话创建策略
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                
                // 会话固化攻击保护
                .sessionFixation().changeSessionId()
                
                // 设置最大会话数（允许多个会话，支持单点登出）
                .maximumSessions(5)
                
                // 当达到最大会话数时，踢出最早的会话
                .maxSessionsPreventsLogin(false)
                
                // 会话过期后的重定向URL
                .expiredUrl("/login?expired=true")
                
                // 设置会话注册表，用于单点登出
                .sessionRegistry(sessionRegistry())
            )
            
            // 配置异常处理
            .exceptionHandling(exception -> exception
                // 未认证时的处理
                .authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        // API请求返回JSON
                        response.setStatus(401);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":\"未认证\",\"message\":\"请先登录\"}");
                    } else {
                        // 页面请求重定向到登录页
                        response.sendRedirect("/login");
                    }
                })
                
                // 访问被拒绝时的处理
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        // API请求返回JSON
                        response.setStatus(403);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":\"访问被拒绝\",\"message\":\"权限不足\"}");
                    } else {
                        // 页面请求重定向到错误页
                        response.sendRedirect("/error?code=403");
                    }
                })
            );
        
        return http.build();
    }
    
    /**
     * 配置认证成功处理器
     * 在登录成功时注册会话到SessionSyncService
     * 
     * @return AuthenticationSuccessHandler
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, 
                                              HttpServletResponse response,
                                              org.springframework.security.core.Authentication authentication) 
                    throws IOException, ServletException {
                
                logger.info("用户登录成功，开始注册会话: {}", authentication.getName());
                
                try {
                    // 获取会话ID
                    String sessionId = request.getSession().getId();
                    String username = authentication.getName();
                    
                    // 获取访问令牌
                    String accessToken = null;
                    if (authentication instanceof OAuth2AuthenticationToken) {
                        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
                        String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
                        String principalName = oauth2Token.getName();
                        
                        logger.debug("获取授权客户端: registrationId={}, principalName={}", registrationId, principalName);
                        
                        try {
                            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                                registrationId, principalName);
                            
                            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                                accessToken = authorizedClient.getAccessToken().getTokenValue();
                                logger.info("登录时成功获取访问令牌: token={}***", 
                                           accessToken.substring(0, Math.min(8, accessToken.length())));
                            } else {
                                logger.warn("登录时无法获取授权客户端或访问令牌");
                            }
                        } catch (Exception e) {
                            logger.error("登录时获取访问令牌失败: {}", e.getMessage(), e);
                        }
                    }
                    
                    // 注册用户会话到SessionSyncService
                    sessionSyncService.registerUserSession(username, sessionId, accessToken);
                    
                    logger.info("会话注册成功: username={}, sessionId={}", username, sessionId);
                    
                } catch (Exception e) {
                    logger.error("会话注册失败: {}", e.getMessage(), e);
                }
                
                // 重定向到目标页面
                String targetUrl = request.getParameter("redirect");
                if (targetUrl == null || targetUrl.isEmpty()) {
                    targetUrl = "/profile";
                }
                
                response.sendRedirect(targetUrl);
            }
        };
    }
    
    /**
     * 配置会话注册表
     * 用于跟踪所有活跃会话，支持单点登出
     * 
     * @return SessionRegistry
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
    
    /**
     * 配置HTTP会话事件发布器
     * 确保会话创建和销毁事件能够被SessionRegistry正确处理
     * 
     * @return HttpSessionEventPublisher
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
} 