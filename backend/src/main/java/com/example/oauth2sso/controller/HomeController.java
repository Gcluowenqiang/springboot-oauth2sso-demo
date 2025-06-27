package com.example.oauth2sso.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 首页控制器
 * 
 * @author Luowenqiang
 * @version 1.0.0
 * @since 2024-12-26
 */
@Controller
public class HomeController {
    
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
    
    /**
     * 首页
     * 
     * @param model 视图模型
     * @param user OAuth2用户信息
     * @param logout 登出参数
     * @return 首页视图
     */
    @GetMapping("/")
    public String index(Model model, 
                       @AuthenticationPrincipal OAuth2User user,
                       @RequestParam(required = false) String logout) {
        
        if (logout != null) {
            String message;
            switch (logout) {
                case "success":
                    message = "您已成功退出本地登录";
                    break;
                case "complete":
                    message = "您已完全退出登录，包括GitHub会话";
                    break;
                case "true":
                default:
                    message = "您已成功登出";
                    break;
            }
            model.addAttribute("message", message);
            model.addAttribute("messageType", "success");
        }
        
        // 如果用户已登录，添加用户信息到模型
        if (user != null) {
            // 注意：不再直接传递OAuth2User对象到模板，而是传递布尔值
            // 模板将使用Spring Security表达式直接访问认证信息
            model.addAttribute("isLoggedIn", true);
            String username = (String) user.getAttribute("login");
            if (username != null) {
                logger.info("已登录用户访问首页: {}", username);
            } else {
                logger.info("已登录用户访问首页: ID={}", (Object) user.getAttribute("id"));
            }
        } else {
            model.addAttribute("isLoggedIn", false);
        }
        
        model.addAttribute("appName", "SpringBoot OAuth2 单点登录示例");
        model.addAttribute("appVersion", "1.0.0");
        
        return "index";
    }
    
    /**
     * 登录页面
     * 
     * @param model 视图模型
     * @param error 错误参数
     * @param expired 会话过期参数
     * @return 登录页面视图
     */
    @GetMapping("/login")
    public String login(Model model,
                       @RequestParam(required = false) String error,
                       @RequestParam(required = false) String expired) {
        
        if (error != null) {
            String errorMessage;
            switch (error) {
                case "oauth2_failed":
                    errorMessage = "GitHub授权失败，请检查网络连接后重试";
                    break;
                case "true":
                default:
                    errorMessage = "登录失败，请重试";
                    break;
            }
            model.addAttribute("errorMessage", errorMessage);
            logger.warn("用户登录失败，错误类型: {}", error);
        }
        
        if (expired != null) {
            model.addAttribute("errorMessage", "会话已过期，请重新登录");
            logger.info("用户会话过期");
        }
        
        model.addAttribute("appName", "SpringBoot OAuth2 单点登录示例");
        
        return "login";
    }
    
    /**
     * 错误页面
     * 
     * @param model 视图模型
     * @param code 错误代码
     * @return 错误页面视图
     */
    @GetMapping("/error")
    public String error(Model model, @RequestParam(required = false) String code) {
        String errorMessage = "发生了未知错误";
        
        if ("403".equals(code)) {
            errorMessage = "访问被拒绝：您没有权限访问此资源";
        } else if ("404".equals(code)) {
            errorMessage = "页面未找到：请求的页面不存在";
        } else if ("500".equals(code)) {
            errorMessage = "服务器内部错误：请稍后重试";
        }
        
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("errorCode", code != null ? code : "未知");
        
        return "error";
    }
} 