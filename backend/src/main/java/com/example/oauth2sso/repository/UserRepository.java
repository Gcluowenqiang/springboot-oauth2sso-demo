package com.example.oauth2sso.repository;

import com.example.oauth2sso.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层接口
 * 
 * @author Luowenqiang
 * @version 1.0.0
 * @since 2024-12-26
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据GitHub ID查找用户（仅查询未删除的用户）
     * 
     * @param githubId GitHub用户ID
     * @return 用户对象
     */
    @Query("SELECT u FROM User u WHERE u.githubId = :githubId AND u.deleteFlag = '0'")
    Optional<User> findByGithubIdAndNotDeleted(@Param("githubId") String githubId);
    
    /**
     * 根据用户名查找用户（仅查询未删除的用户）
     * 
     * @param username 用户名
     * @return 用户对象
     */
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.deleteFlag = '0'")
    Optional<User> findByUsernameAndNotDeleted(@Param("username") String username);
    
    /**
     * 根据邮箱查找用户（仅查询未删除的用户）
     * 
     * @param email 邮箱地址
     * @return 用户对象
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deleteFlag = '0'")
    Optional<User> findByEmailAndNotDeleted(@Param("email") String email);
    
    /**
     * 查询所有未删除的用户
     * 
     * @return 用户列表
     */
    @Query("SELECT u FROM User u WHERE u.deleteFlag = '0' ORDER BY u.createdAt DESC")
    List<User> findAllNotDeleted();
    
    /**
     * 根据GitHub ID查找用户（包括已删除的用户）
     * 
     * @param githubId GitHub用户ID
     * @return 用户对象
     */
    Optional<User> findByGithubId(String githubId);
    
    /**
     * 检查GitHub ID是否已存在（仅检查未删除的用户）
     * 
     * @param githubId GitHub用户ID
     * @return 是否存在
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.githubId = :githubId AND u.deleteFlag = '0'")
    boolean existsByGithubIdAndNotDeleted(@Param("githubId") String githubId);
    
    /**
     * 检查用户名是否已存在（仅检查未删除的用户）
     * 
     * @param username 用户名
     * @return 是否存在
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username AND u.deleteFlag = '0'")
    boolean existsByUsernameAndNotDeleted(@Param("username") String username);
    
    /**
     * 检查邮箱是否已存在（仅检查未删除的用户）
     * 
     * @param email 邮箱地址
     * @return 是否存在
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.deleteFlag = '0'")
    boolean existsByEmailAndNotDeleted(@Param("email") String email);
    
    /**
     * 根据用户名模糊查询用户（仅查询未删除的用户）
     * 
     * @param username 用户名关键字
     * @return 用户列表
     */
    @Query("SELECT u FROM User u WHERE u.username LIKE %:username% AND u.deleteFlag = '0' ORDER BY u.createdAt DESC")
    List<User> findByUsernameContainingAndNotDeleted(@Param("username") String username);
    
    /**
     * 统计未删除用户总数
     * 
     * @return 用户总数
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deleteFlag = '0'")
    long countNotDeleted();
} 