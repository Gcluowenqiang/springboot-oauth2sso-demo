package com.example.oauth2sso.controller;

import com.example.oauth2sso.dto.UserDTO;
import com.example.oauth2sso.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户信息控制器
 * 
 * @author Luowenqiang
 * @version 1.0.0
 * @since 2024-12-26
 */
@Controller
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private UserService userService;
    
    /**
     * 用户信息页面
     * 
     * @param model 视图模型
     * @param oauth2User OAuth2用户信息
     * @return 用户信息页面视图
     */
    @GetMapping("/profile")
    public String profile(Model model, @AuthenticationPrincipal OAuth2User oauth2User) {
        try {
            if (oauth2User == null) {
                logger.warn("用户未认证，重定向到登录页面");
                return "redirect:/login";
            }
            
            // 安全获取GitHub ID
            Object idObj = oauth2User.getAttribute("id");
            String githubId = idObj != null ? String.valueOf(idObj) : null;
            
            if (githubId == null || githubId.isEmpty()) {
                logger.error("无法获取用户GitHub ID，OAuth2用户属性: {}", oauth2User.getAttributes());
                model.addAttribute("errorMessage", "无法获取用户标识信息");
                return "profile";
            }
            
            Optional<UserDTO> userDTO = userService.findByGithubId(githubId);
            
            if (userDTO.isPresent()) {
                UserDTO user = userDTO.get();
                model.addAttribute("user", user);
                // 安全的日志记录
                String username = user.getUsername();
                if (username != null) {
                    logger.info("用户访问个人信息页面: {}", username);
                } else {
                    logger.info("用户访问个人信息页面: GitHub ID={}", githubId);
                }
            } else {
                logger.warn("未找到用户信息: GitHub ID={}", githubId);
                model.addAttribute("errorMessage", "未找到用户信息，请尝试重新登录");
            }
            
            // 添加OAuth2原始属性（用于调试，生产环境可删除）
            if (logger.isDebugEnabled()) {
                model.addAttribute("oauth2Attributes", oauth2User.getAttributes());
            }
            
            return "profile";
            
        } catch (Exception e) {
            logger.error("访问个人信息页面时发生异常", e);
            model.addAttribute("errorMessage", "系统异常，请稍后重试");
            return "profile";
        }
    }
    
    /**
     * 获取当前用户信息API
     * 
     * @param oauth2User OAuth2用户信息
     * @return 用户信息JSON
     */
    @GetMapping("/api/user/current")
    @ResponseBody
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal OAuth2User oauth2User) {
        try {
            if (oauth2User == null) {
                return ResponseEntity.status(401).body(Map.of("error", "未认证", "message", "请先登录"));
            }
            
            Object idObj = oauth2User.getAttribute("id");
            String githubId = idObj != null ? String.valueOf(idObj) : null;
            
            if (githubId == null) {
                return ResponseEntity.status(400).body(Map.of("error", "无效用户", "message", "无法获取用户标识"));
            }
            
            Optional<UserDTO> userDTO = userService.findByGithubId(githubId);
            
            if (userDTO.isPresent()) {
                return ResponseEntity.ok(userDTO.get());
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "用户不存在", "message", "未找到用户信息"));
            }
            
        } catch (Exception e) {
            logger.error("获取当前用户信息时发生异常", e);
            return ResponseEntity.status(500).body(Map.of("error", "系统异常", "message", "服务器内部错误"));
        }
    }
    
    /**
     * 更新用户信息API
     * 
     * @param userDTO 用户信息
     * @param oauth2User OAuth2用户信息
     * @return 更新结果
     */
    @PostMapping("/api/user/update")
    @ResponseBody
    public ResponseEntity<?> updateUser(@RequestBody UserDTO userDTO, 
                                       @AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证", "message", "请先登录"));
        }
        
        try {
            Object idObj = oauth2User.getAttribute("id");
            String githubId = idObj != null ? String.valueOf(idObj) : null;
            
            if (githubId == null) {
                return ResponseEntity.status(400).body(Map.of("error", "无效用户", "message", "无法获取用户标识"));
            }
            
            Optional<UserDTO> currentUser = userService.findByGithubId(githubId);
            
            if (currentUser.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "用户不存在", "message", "未找到用户信息"));
            }
            
            // 只允许用户更新自己的信息
            if (!githubId.equals(currentUser.get().getGithubId())) {
                return ResponseEntity.status(403).body(Map.of("error", "权限不足", "message", "只能更新自己的信息"));
            }
            
            // 设置用户ID，确保更新正确的用户
            userDTO.setId(currentUser.get().getId());
            userDTO.setGithubId(currentUser.get().getGithubId());
            
            UserDTO updatedUser = userService.updateUser(userDTO);
            
            logger.info("用户更新信息成功: {}", updatedUser.getUsername());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "用户信息更新成功",
                "user", updatedUser
            ));
            
        } catch (Exception e) {
            logger.error("更新用户信息失败", e);
            return ResponseEntity.status(500).body(Map.of("error", "更新失败", "message", e.getMessage()));
        }
    }
    
    /**
     * 获取所有用户API（管理员功能）
     * 
     * @param oauth2User OAuth2用户信息
     * @return 用户列表
     */
    @GetMapping("/api/users")
    @ResponseBody
    public ResponseEntity<?> getAllUsers(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证", "message", "请先登录"));
        }
        
        try {
            List<UserDTO> users = userService.findAll();
            long totalCount = userService.count();
            
            Map<String, Object> response = new HashMap<>();
            response.put("users", users);
            response.put("total", totalCount);
            response.put("count", users.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取用户列表失败", e);
            return ResponseEntity.status(500).body(Map.of("error", "获取失败", "message", e.getMessage()));
        }
    }
    
    /**
     * 搜索用户API
     * 
     * @param username 用户名关键字
     * @param oauth2User OAuth2用户信息
     * @return 搜索结果
     */
    @GetMapping("/api/users/search")
    @ResponseBody
    public ResponseEntity<?> searchUsers(@RequestParam String username,
                                        @AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证", "message", "请先登录"));
        }
        
        try {
            List<UserDTO> users = userService.searchByUsername(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("users", users);
            response.put("count", users.size());
            response.put("keyword", username);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("搜索用户失败", e);
            return ResponseEntity.status(500).body(Map.of("error", "搜索失败", "message", e.getMessage()));
        }
    }
    
    /**
     * 用户统计信息API
     * 
     * @param oauth2User OAuth2用户信息
     * @return 统计信息
     */
    @GetMapping("/api/users/stats")
    @ResponseBody
    public ResponseEntity<?> getUserStats(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证", "message", "请先登录"));
        }
        
        try {
            long totalUsers = userService.count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("获取用户统计信息失败", e);
            return ResponseEntity.status(500).body(Map.of("error", "获取失败", "message", e.getMessage()));
        }
    }
} 