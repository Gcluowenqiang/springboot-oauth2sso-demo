package com.example.oauth2sso.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 
 * @author Luowenqiang
 * @version 1.0.0
 * @since 2024-12-26
 */
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "github_id", unique = true, nullable = false, length = 50)
    @NotBlank(message = "GitHub ID不能为空")
    @Size(max = 50, message = "GitHub ID长度不能超过50")
    private String githubId;
    
    @Column(name = "username", nullable = false, length = 100)
    @NotBlank(message = "用户名不能为空")
    @Size(max = 100, message = "用户名长度不能超过100")
    private String username;
    
    @Column(name = "email", length = 255)
    @Email(message = "邮箱格式不正确")
    @Size(max = 255, message = "邮箱长度不能超过255")
    private String email;
    
    @Column(name = "avatar_url", length = 500)
    @Size(max = 500, message = "头像URL长度不能超过500")
    private String avatarUrl;
    
    @Column(name = "name", length = 200)
    @Size(max = 200, message = "姓名长度不能超过200")
    private String name;
    
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;
    
    @Column(name = "location", length = 255)
    @Size(max = 255, message = "地址长度不能超过255")
    private String location;
    
    @Column(name = "company", length = 255)
    @Size(max = 255, message = "公司长度不能超过255")
    private String company;
    
    @Column(name = "blog", length = 500)
    @Size(max = 500, message = "博客地址长度不能超过500")
    private String blog;
    
    @Column(name = "public_repos", columnDefinition = "INT DEFAULT 0")
    private Integer publicRepos = 0;
    
    @Column(name = "followers", columnDefinition = "INT DEFAULT 0")
    private Integer followers = 0;
    
    @Column(name = "following", columnDefinition = "INT DEFAULT 0")
    private Integer following = 0;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "delete_flag", length = 1, columnDefinition = "CHAR(1) DEFAULT '0'")
    private String deleteFlag = "0";
    
    @Column(name = "delete_user", length = 100)
    private String deleteUser;
    
    @Column(name = "delete_time")
    private LocalDateTime deleteTime;
    
    // 默认构造函数
    public User() {}
    
    // 带参构造函数
    public User(String githubId, String username, String email) {
        this.githubId = githubId;
        this.username = username;
        this.email = email;
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
    
    public String getDeleteFlag() {
        return deleteFlag;
    }
    
    public void setDeleteFlag(String deleteFlag) {
        this.deleteFlag = deleteFlag;
    }
    
    public String getDeleteUser() {
        return deleteUser;
    }
    
    public void setDeleteUser(String deleteUser) {
        this.deleteUser = deleteUser;
    }
    
    public LocalDateTime getDeleteTime() {
        return deleteTime;
    }
    
    public void setDeleteTime(LocalDateTime deleteTime) {
        this.deleteTime = deleteTime;
    }
    
    /**
     * 软删除用户
     */
    public void softDelete(String deleteUser) {
        this.deleteFlag = "1";
        this.deleteUser = deleteUser;
        this.deleteTime = LocalDateTime.now();
    }
    
    /**
     * 恢复软删除用户
     */
    public void restoreDelete() {
        this.deleteFlag = "0";
        this.deleteUser = null;
        this.deleteTime = null;
    }
    
    /**
     * 更新最后登录时间
     */
    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", githubId='" + githubId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", deleteFlag='" + deleteFlag + '\'' +
                '}';
    }
} 