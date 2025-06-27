package com.example.oauth2sso.service;

import com.example.oauth2sso.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;

/**
 * 自定义OAuth2用户服务
 * 处理GitHub OAuth2认证后的用户信息
 * 
 * @author Luowenqiang
 * @version 1.0.0
 * @since 2024-12-26
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    
    @Autowired
    private UserService userService;
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 调用父类方法获取OAuth2User对象
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        try {
            // 处理用户信息
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception e) {
            logger.error("处理OAuth2用户信息时发生错误", e);
            throw new OAuth2AuthenticationException("处理OAuth2用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理OAuth2用户信息
     * 
     * @param userRequest OAuth2用户请求
     * @param oAuth2User OAuth2用户对象
     * @return 处理后的OAuth2User对象
     */
    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        logger.info("处理OAuth2用户登录, 提供商: {}, 用户信息: {}", (Object) registrationId, (Object) attributes);
        
        // 根据OAuth2提供商处理用户信息
        UserDTO userDTO;
        if ("github".equalsIgnoreCase(registrationId)) {
            userDTO = processGitHubUser(attributes);
        } else {
            throw new OAuth2AuthenticationException("不支持的OAuth2提供商: " + registrationId);
        }
        
        // 保存或更新用户信息到数据库
        UserDTO savedUser = userService.processOAuth2Login(
                userDTO.getGithubId(), 
                userDTO.getUsername(), 
                userDTO.getEmail(),
                userDTO.getAvatarUrl(), 
                userDTO.getName(), 
                userDTO.getBio(),
                userDTO.getLocation(), 
                userDTO.getCompany(), 
                userDTO.getBlog(),
                userDTO.getPublicRepos(), 
                userDTO.getFollowers(), 
                userDTO.getFollowing()
        );
        
        logger.info("用户OAuth2登录成功: {}", (Object) savedUser.getUsername());
        
        // 创建包含用户信息的OAuth2User对象
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                getNameAttributeKey(registrationId)
        );
    }
    
    /**
     * 处理GitHub用户信息
     * 
     * @param attributes GitHub返回的用户属性
     * @return UserDTO对象
     */
    private UserDTO processGitHubUser(Map<String, Object> attributes) {
        // GitHub API返回的用户信息字段
        String githubId = String.valueOf(attributes.get("id"));
        String username = (String) attributes.get("login");
        String email = (String) attributes.get("email");
        String avatarUrl = (String) attributes.get("avatar_url");
        String name = (String) attributes.get("name");
        String bio = (String) attributes.get("bio");
        String location = (String) attributes.get("location");
        String company = (String) attributes.get("company");
        String blog = (String) attributes.get("blog");
        
        // 数值类型字段处理
        Integer publicRepos = getIntegerValue(attributes.get("public_repos"));
        Integer followers = getIntegerValue(attributes.get("followers"));
        Integer following = getIntegerValue(attributes.get("following"));
        
        // 验证必需字段
        if (!StringUtils.hasText(githubId) || !StringUtils.hasText(username)) {
            throw new OAuth2AuthenticationException("GitHub用户信息不完整：缺少ID或用户名");
        }
        
        // 创建UserDTO对象
        UserDTO userDTO = new UserDTO(githubId, username, email);
        userDTO.setAvatarUrl(avatarUrl);
        userDTO.setName(name);
        userDTO.setBio(bio);
        userDTO.setLocation(location);
        userDTO.setCompany(company);
        userDTO.setBlog(blog);
        userDTO.setPublicRepos(publicRepos);
        userDTO.setFollowers(followers);
        userDTO.setFollowing(following);
        
        logger.debug("处理GitHub用户信息: {}", (Object) userDTO.toString());
        
        return userDTO;
    }
    
    /**
     * 获取用户名属性键
     * 
     * @param registrationId OAuth2注册ID
     * @return 用户名属性键
     */
    private String getNameAttributeKey(String registrationId) {
        switch (registrationId.toLowerCase()) {
            case "github":
                return "login";
            case "google":
                return "sub";
            default:
                return "sub";
        }
    }
    
    /**
     * 安全地将Object转换为Integer
     * 
     * @param value 要转换的值
     * @return Integer值，如果转换失败则返回0
     */
    private Integer getIntegerValue(Object value) {
        if (value == null) {
            return 0;
        }
        
        try {
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
        } catch (NumberFormatException e) {
            logger.warn("无法将值转换为Integer: {}", (Object) value);
        }
        
        return 0;
    }
} 