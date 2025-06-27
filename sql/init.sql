-- =================================================
-- SpringBoot OAuth2 单点登录数据库初始化脚本
-- 数据库: OAuth2db
-- 版本: 1.0
-- 创建时间: 2024-12-26
-- =================================================

USE OAuth2db;

-- 删除已存在的表（如果存在）
DROP TABLE IF EXISTS `users`;

-- 创建用户表
CREATE TABLE `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `github_id` VARCHAR(50) NOT NULL UNIQUE COMMENT 'GitHub用户ID',
    `username` VARCHAR(100) NOT NULL COMMENT '用户名',
    `email` VARCHAR(255) COMMENT '邮箱地址',
    `avatar_url` VARCHAR(500) COMMENT '头像URL',
    `name` VARCHAR(200) COMMENT '真实姓名',
    `bio` TEXT COMMENT '个人简介',
    `location` VARCHAR(255) COMMENT '所在地',
    `company` VARCHAR(255) COMMENT '公司',
    `blog` VARCHAR(500) COMMENT '博客地址',
    `public_repos` INT DEFAULT 0 COMMENT '公开仓库数量',
    `followers` INT DEFAULT 0 COMMENT '关注者数量',
    `following` INT DEFAULT 0 COMMENT '关注数量',
    `last_login` TIMESTAMP NULL COMMENT '最后登录时间',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `delete_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
    `delete_user` VARCHAR(100) COMMENT '删除人',
    `delete_time` TIMESTAMP NULL COMMENT '删除时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

-- 创建索引
CREATE INDEX `idx_github_id` ON `users` (`github_id`);
CREATE INDEX `idx_username` ON `users` (`username`);
CREATE INDEX `idx_email` ON `users` (`email`);
CREATE INDEX `idx_delete_flag` ON `users` (`delete_flag`);
CREATE INDEX `idx_created_at` ON `users` (`created_at`);
CREATE INDEX `idx_last_login` ON `users` (`last_login`);

-- 插入测试数据（可选）
INSERT INTO `users` (
    `github_id`, `username`, `email`, `avatar_url`, `name`, 
    `bio`, `location`, `company`, `public_repos`, `followers`, `following`
) VALUES (
    '123456', 'testuser', 'test@example.com', 'https://avatars.githubusercontent.com/u/123456?v=4',
    'Test User', '这是一个测试用户', 'Beijing, China', 'Example Company', 10, 50, 30
);

-- 查看表结构
DESCRIBE `users`;

-- 查看索引
SHOW INDEX FROM `users`;

SELECT 'OAuth2 数据库初始化完成！' AS message; 