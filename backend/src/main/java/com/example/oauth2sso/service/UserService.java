package com.example.oauth2sso.service;

import com.example.oauth2sso.dto.UserDTO;
import com.example.oauth2sso.entity.User;
import com.example.oauth2sso.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户业务逻辑服务类
 * 
 * @author Luowenqiang
 * @version 1.0.0
 * @since 2024-12-26
 */
@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 根据GitHub ID查找用户
     * 
     * @param githubId GitHub用户ID
     * @return 用户对象
     */
    @Transactional(readOnly = true)
    public Optional<UserDTO> findByGithubId(String githubId) {
        if (!StringUtils.hasText(githubId)) {
            return Optional.empty();
        }
        
        return userRepository.findByGithubIdAndNotDeleted(githubId)
                .map(UserDTO::fromEntity);
    }
    
    /**
     * 根据用户名查找用户
     * 
     * @param username 用户名
     * @return 用户对象
     */
    @Transactional(readOnly = true)
    public Optional<UserDTO> findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }
        
        return userRepository.findByUsernameAndNotDeleted(username)
                .map(UserDTO::fromEntity);
    }
    
    /**
     * 根据ID查找用户
     * 
     * @param id 用户ID
     * @return 用户对象
     */
    @Transactional(readOnly = true)
    public Optional<UserDTO> findById(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        
        return userRepository.findById(id)
                .filter(user -> "0".equals(user.getDeleteFlag()))
                .map(UserDTO::fromEntity);
    }
    
    /**
     * 查询所有用户
     * 
     * @return 用户列表
     */
    @Transactional(readOnly = true)
    public List<UserDTO> findAll() {
        return userRepository.findAllNotDeleted()
                .stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 创建新用户
     * 
     * @param userDTO 用户数据传输对象
     * @return 创建的用户对象
     */
    public UserDTO createUser(UserDTO userDTO) {
        if (userDTO == null || !StringUtils.hasText(userDTO.getGithubId())) {
            throw new IllegalArgumentException("用户信息不能为空，且GitHub ID必须提供");
        }
        
        // 检查GitHub ID是否已存在
        if (userRepository.existsByGithubIdAndNotDeleted(userDTO.getGithubId())) {
            throw new IllegalArgumentException("GitHub ID已存在: " + userDTO.getGithubId());
        }
        
        User user = userDTO.toEntity();
        user.setDeleteFlag("0");
        user.updateLastLogin();
        
        User savedUser = userRepository.save(user);
        logger.info("创建新用户成功: {}", (Object) savedUser.toString());
        
        return UserDTO.fromEntity(savedUser);
    }
    
    /**
     * 更新用户信息
     * 
     * @param userDTO 用户数据传输对象
     * @return 更新后的用户对象
     */
    public UserDTO updateUser(UserDTO userDTO) {
        if (userDTO == null || userDTO.getId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        Optional<User> existingUserOpt = userRepository.findById(userDTO.getId());
        if (existingUserOpt.isEmpty() || !"0".equals(existingUserOpt.get().getDeleteFlag())) {
            throw new IllegalArgumentException("用户不存在或已被删除: " + userDTO.getId());
        }
        
        User existingUser = existingUserOpt.get();
        
        // 更新用户信息（保留创建时间和删除标志）
        if (StringUtils.hasText(userDTO.getUsername())) {
            existingUser.setUsername(userDTO.getUsername());
        }
        if (StringUtils.hasText(userDTO.getEmail())) {
            existingUser.setEmail(userDTO.getEmail());
        }
        if (StringUtils.hasText(userDTO.getAvatarUrl())) {
            existingUser.setAvatarUrl(userDTO.getAvatarUrl());
        }
        if (StringUtils.hasText(userDTO.getName())) {
            existingUser.setName(userDTO.getName());
        }
        existingUser.setBio(userDTO.getBio());
        existingUser.setLocation(userDTO.getLocation());
        existingUser.setCompany(userDTO.getCompany());
        existingUser.setBlog(userDTO.getBlog());
        
        if (userDTO.getPublicRepos() != null) {
            existingUser.setPublicRepos(userDTO.getPublicRepos());
        }
        if (userDTO.getFollowers() != null) {
            existingUser.setFollowers(userDTO.getFollowers());
        }
        if (userDTO.getFollowing() != null) {
            existingUser.setFollowing(userDTO.getFollowing());
        }
        
        User savedUser = userRepository.save(existingUser);
        logger.info("更新用户信息成功: {}", (Object) savedUser.toString());
        
        return UserDTO.fromEntity(savedUser);
    }
    
    /**
     * OAuth2登录时处理用户信息
     * 创建新用户或更新现有用户的登录时间和信息
     * 
     * @param githubId GitHub用户ID
     * @param username 用户名
     * @param email 邮箱
     * @param avatarUrl 头像URL
     * @param name 真实姓名
     * @param bio 个人简介
     * @param location 地址
     * @param company 公司
     * @param blog 博客
     * @param publicRepos 公开仓库数
     * @param followers 关注者数
     * @param following 关注数
     * @return 用户对象
     */
    public UserDTO processOAuth2Login(String githubId, String username, String email, 
                                     String avatarUrl, String name, String bio, 
                                     String location, String company, String blog,
                                     Integer publicRepos, Integer followers, Integer following) {
        
        Optional<User> existingUserOpt = userRepository.findByGithubIdAndNotDeleted(githubId);
        
        User user;
        if (existingUserOpt.isPresent()) {
            // 更新现有用户信息
            user = existingUserOpt.get();
            user.setUsername(username);
            user.setEmail(email);
            user.setAvatarUrl(avatarUrl);
            user.setName(name);
            user.setBio(bio);
            user.setLocation(location);
            user.setCompany(company);
            user.setBlog(blog);
            user.setPublicRepos(publicRepos);
            user.setFollowers(followers);
            user.setFollowing(following);
            user.updateLastLogin();
            
            logger.info("更新现有用户登录信息: {}", (Object) githubId);
        } else {
            // 创建新用户
            user = new User();
            user.setGithubId(githubId);
            user.setUsername(username);
            user.setEmail(email);
            user.setAvatarUrl(avatarUrl);
            user.setName(name);
            user.setBio(bio);
            user.setLocation(location);
            user.setCompany(company);
            user.setBlog(blog);
            user.setPublicRepos(publicRepos);
            user.setFollowers(followers);
            user.setFollowing(following);
            user.setDeleteFlag("0");
            user.updateLastLogin();
            
            logger.info("创建新用户: {}", (Object) githubId);
        }
        
        User savedUser = userRepository.save(user);
        return UserDTO.fromEntity(savedUser);
    }
    
    /**
     * 更新用户最后登录时间
     * 
     * @param githubId GitHub用户ID
     */
    public void updateLastLogin(String githubId) {
        if (!StringUtils.hasText(githubId)) {
            return;
        }
        
        Optional<User> userOpt = userRepository.findByGithubIdAndNotDeleted(githubId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.updateLastLogin();
            userRepository.save(user);
            logger.debug("更新用户最后登录时间: {}", (Object) githubId);
        }
    }
    
    /**
     * 软删除用户
     * 
     * @param id 用户ID
     * @param deleteUser 删除操作人
     */
    public void deleteUser(Long id, String deleteUser) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty() || !"0".equals(userOpt.get().getDeleteFlag())) {
            throw new IllegalArgumentException("用户不存在或已被删除: " + id);
        }
        
        User user = userOpt.get();
        user.softDelete(deleteUser);
        userRepository.save(user);
        
        logger.info("软删除用户成功: {}, 操作人: {}", (Object) id, (Object) deleteUser);
    }
    
    /**
     * 恢复软删除用户
     * 
     * @param id 用户ID
     */
    public void restoreUser(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("用户不存在: " + id);
        }
        
        User user = userOpt.get();
        if ("0".equals(user.getDeleteFlag())) {
            throw new IllegalArgumentException("用户未被删除，无需恢复: " + id);
        }
        
        user.restoreDelete();
        userRepository.save(user);
        
        logger.info("恢复删除用户成功: {}", (Object) id);
    }
    
    /**
     * 检查GitHub ID是否存在
     * 
     * @param githubId GitHub用户ID
     * @return 是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByGithubId(String githubId) {
        if (!StringUtils.hasText(githubId)) {
            return false;
        }
        return userRepository.existsByGithubIdAndNotDeleted(githubId);
    }
    
    /**
     * 检查用户名是否存在
     * 
     * @param username 用户名
     * @return 是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        return userRepository.existsByUsernameAndNotDeleted(username);
    }
    
    /**
     * 根据用户名模糊查询
     * 
     * @param username 用户名关键字
     * @return 用户列表
     */
    @Transactional(readOnly = true)
    public List<UserDTO> searchByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return findAll();
        }
        
        return userRepository.findByUsernameContainingAndNotDeleted(username)
                .stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 统计用户总数
     * 
     * @return 用户总数
     */
    @Transactional(readOnly = true)
    public long count() {
        return userRepository.countNotDeleted();
    }
} 