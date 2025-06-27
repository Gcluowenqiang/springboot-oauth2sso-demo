package com.example.oauth2sso.dto;

import com.example.oauth2sso.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 用户数据传输对象
 * 
 * @author Luowenqiang
 * @version 1.0.0
 * @since 2024-12-26
 */
public class UserDTO {
    
    private Long id;
    
    @NotBlank(message = "GitHub ID不能为空")
    @Size(max = 50, message = "GitHub ID长度不能超过50")
    private String githubId;
    
    @NotBlank(message = "用户名不能为空")
    @Size(max = 100, message = "用户名长度不能超过100")
    private String username;
    
    @Email(message = "邮箱格式不正确")
    @Size(max = 255, message = "邮箱长度不能超过255")
    private String email;
    
    @Size(max = 500, message = "头像URL长度不能超过500")
    private String avatarUrl;
    
    @Size(max = 200, message = "姓名长度不能超过200")
    private String name;
    
    private String bio;
    
    @Size(max = 255, message = "地址长度不能超过255")
    private String location;
    
    @Size(max = 255, message = "公司长度不能超过255")
    private String company;
    
    @Size(max = 500, message = "博客地址长度不能超过500")
    private String blog;
    
    private Integer publicRepos;
    private Integer followers;
    private Integer following;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 默认构造函数
    public UserDTO() {}
    
    // 带参构造函数
    public UserDTO(String githubId, String username, String email) {
        this.githubId = githubId;
        this.username = username;
        this.email = email;
    }
    
    /**
     * 从User实体转换为UserDTO
     * 
     * @param user 用户实体
     * @return UserDTO对象
     */
    public static UserDTO fromEntity(User user) {
        if (user == null) {
            return null;
        }
        
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setGithubId(user.getGithubId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setName(user.getName());
        dto.setBio(user.getBio());
        dto.setLocation(user.getLocation());
        dto.setCompany(user.getCompany());
        dto.setBlog(user.getBlog());
        dto.setPublicRepos(user.getPublicRepos());
        dto.setFollowers(user.getFollowers());
        dto.setFollowing(user.getFollowing());
        dto.setLastLogin(user.getLastLogin());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        
        return dto;
    }
    
    /**
     * 将UserDTO转换为User实体
     * 
     * @return User实体对象
     */
    public User toEntity() {
        User user = new User();
        user.setId(this.id);
        user.setGithubId(this.githubId);
        user.setUsername(this.username);
        user.setEmail(this.email);
        user.setAvatarUrl(this.avatarUrl);
        user.setName(this.name);
        user.setBio(this.bio);
        user.setLocation(this.location);
        user.setCompany(this.company);
        user.setBlog(this.blog);
        user.setPublicRepos(this.publicRepos);
        user.setFollowers(this.followers);
        user.setFollowing(this.following);
        user.setLastLogin(this.lastLogin);
        
        return user;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getGithubId() {
        return githubId;
    }
    
    public void setGithubId(String githubId) {
        this.githubId = githubId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getBio() {
        return bio;
    }
    
    public void setBio(String bio) {
        this.bio = bio;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getCompany() {
        return company;
    }
    
    public void setCompany(String company) {
        this.company = company;
    }
    
    public String getBlog() {
        return blog;
    }
    
    public void setBlog(String blog) {
        this.blog = blog;
    }
    
    public Integer getPublicRepos() {
        return publicRepos;
    }
    
    public void setPublicRepos(Integer publicRepos) {
        this.publicRepos = publicRepos;
    }
    
    public Integer getFollowers() {
        return followers;
    }
    
    public void setFollowers(Integer followers) {
        this.followers = followers;
    }
    
    public Integer getFollowing() {
        return following;
    }
    
    public void setFollowing(Integer following) {
        this.following = following;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", githubId='" + githubId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
} 