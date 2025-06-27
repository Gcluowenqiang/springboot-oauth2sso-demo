# SpringBoot OAuth2 单点登录示例 (GitHub认证)

## 📋 项目概述

本项目是一个基于Spring Boot的OAuth2单点登录示例，使用GitHub作为OAuth2提供商进行用户认证，数据存储使用MySQL8数据库。支持令牌撤销、会话同步、跨域协调和实时通知。系统已完善会话注册机制、令牌提取功能、WebSocket通知和调试诊断工具。

## 🛠️ 技术栈

- **Java**: JDK 21
- **框架**: Spring Boot 3.x
- **安全**: Spring Security + OAuth2 Client
- **数据库**: MySQL 8.0
- **ORM**: Spring Data JPA + Hibernate
- **构建工具**: Maven
- **模板引擎**: Thymeleaf
- **实时通信**: WebSocket + SockJS
- **编码**: UTF-8

## 🏗️ 完整项目结构

```
springBoot单点登录(OAuth2)/
├── README.md                           # 项目说明文档
├── backend/                            # 主项目目录
│   ├── pom.xml                         # Maven配置文件
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/example/oauth2sso/
│   │   │   │       ├── OAuth2SsoApplication.java          # 主启动类
│   │   │   │       ├── config/
│   │   │   │       │   ├── SecurityConfig.java            # 安全配置
│   │   │   │       │   ├── WebSocketConfig.java           # WebSocket配置
│   │   │   │       │   └── HttpClientConfig.java          # HTTP客户端配置（网络优化）
│   │   │   │       ├── controller/
│   │   │   │       │   ├── HomeController.java            # 首页控制器
│   │   │   │       │   ├── UserController.java            # 用户信息控制器
│   │   │   │       │   ├── SingleSignOutController.java   # 真正的单点登出控制器
│   │   │   │       │   └── DebugController.java           # 调试诊断控制器
│   │   │   │       ├── entity/
│   │   │   │       │   └── User.java                      # 用户实体类
│   │   │   │       ├── handler/
│   │   │   │       │   └── LogoutWebSocketHandler.java    # WebSocket处理器
│   │   │   │       ├── repository/
│   │   │   │       │   └── UserRepository.java            # 用户数据访问层
│   │   │   │       ├── service/
│   │   │   │       │   ├── UserService.java               # 用户业务逻辑
│   │   │   │       │   ├── CustomOAuth2UserService.java   # 自定义OAuth2用户服务
│   │   │   │       │   ├── OAuth2TokenService.java        # OAuth2令牌管理服务（已优化）
│   │   │   │       │   ├── SessionSyncService.java        # 会话同步服务
│   │   │   │       │   └── LogoutNotificationService.java # 登出通知服务
│   │   │   │       └── dto/
│   │   │   │           └── UserDTO.java                   # 用户数据传输对象
│   │   │   └── resources/
│   │   │       ├── application.yml                        # 应用配置文件
│   │   │       ├── application-dev.yml                    # 开发环境配置
│   │   │       ├── rebel.xml                              # JRebel配置
│   │   │       ├── static/                                # 静态资源
│   │   │       │   ├── css/
│   │   │       │   │   └── style.css                      # 样式文件
│   │   │       │   └── js/
│   │   │       │       └── main.js                        # 前端脚本
│   │   │       └── templates/                             # Thymeleaf模板
│   │   │           ├── index.html                         # 首页模板
│   │   │           ├── login.html                         # 登录页面
│   │   │           ├── profile.html                       # 用户信息页面
│   │   │           ├── sso-logout-confirm.html            # 单点登出确认页面
│   │   │           ├── sso-logout-result.html             # 单点登出结果页面
│   │   │           ├── debug.html                         # 系统调试诊断页面
│   │   │           └── error.html                         # 错误页面
│   │   └── test/
│   │       └── java/
│   │           └── com/example/oauth2sso/
│   │               └── OAuth2SsoApplicationTests.java     # 单元测试
├── docs/                               # 文档目录
│   ├── setup-guide.md                  # 环境搭建指南
│   ├── github-oauth-setup.md           # GitHub OAuth应用配置指南
│   ├── environment-setup.md            # 环境变量配置指南
│   └── api-documentation.md            # API文档
├── student/                            # 技术学习文档目录
│   ├── OAuth2登出机制技术文档.md        # OAuth2登出机制详细文档
│   ├── 真正的单点登出实现技术文档.md     # 真正单点登出完整实现方案
│   ├── Spring_Data_JPA_Hibernate技术栈详解.md
│   └── Spring_Data_JPA_vs_MyBatis_Plus_技术选型对比分析.md
├── logs/                               # 应用日志目录
│   └── oauth2-sso.log                  # 应用运行日志
└── sql/
    └── init.sql                        # 数据库初始化脚本
```

## ✨ 功能特性

- ✅ GitHub OAuth2 单点登录
- ✅ **真正的单点登出**（令牌撤销+会话同步+实时通知）
- ✅ 多层级登出模式（本地/完整/全局登出）
- ✅ WebSocket实时通知系统
- ✅ 跨设备会话管理
- ✅ 用户信息持久化存储
- ✅ 会话管理和安全控制
- ✅ 响应式前端界面
- ✅ 完整的错误处理和异常管理
- ✅ 系统调试诊断工具
- ✅ 会话状态实时监控
- ✅ WebSocket连接状态检测
- ✅ 开发/生产环境配置分离
- ✅ RESTful API接口
- ✅ 单元测试覆盖
- ✅ 详细的技术文档和学习资料

## 🚀 快速开始

### 1. 环境准备
- JDK 21
- Maven 3.6+
- MySQL 8.0+

### 2. 数据库配置
- 数据库名: `oauth2db`
- 端口: 3306

### 3. GitHub OAuth应用配置
1. 登录GitHub，进入Settings → Developer settings → OAuth Apps
2. 创建新的OAuth App
3. 设置回调URL: `http://localhost:8080/login/oauth2/code/github`

### 4. 运行应用
```bash
cd backend
mvn spring-boot:run
```

### 5. 访问应用
- 应用首页: http://localhost:8080
- 登录页面: http://localhost:8080/login
- 真正的单点登出: http://localhost:8080/sso/logout
- 系统调试诊断: http://localhost:8080/debug

## 🔐 单点登出功能

### 登出类型对比

| 登出类型 | 本地会话 | 其他会话 | OAuth2令牌 | 适用场景 |
|---------|---------|---------|------------|----------|
| 本地登出 | ✅ 清除 | ❌ 保留 | ❌ 保留 | 快速切换用户 |
| 完整登出 | ✅ 清除 | ✅ 清除 | ❌ 保留 | 常规安全要求（推荐） |
| 全局登出 | ✅ 清除 | ✅ 清除 | ✅ 撤销 | 最高安全级别 |

### 核心特性
- **令牌撤销机制**: 调用GitHub API立即撤销OAuth2令牌
- **会话同步通知**: 跨设备、跨浏览器的会话统一管理
- **跨域登出协调**: 支持多种登出模式满足不同安全需求
- **WebSocket实时通知**: 其他设备收到即时登出通知

### ⚠️ OAuth2令牌撤销注意事项

**问题描述**: 在某些情况下，执行"全局登出"后可能出现重新登录时连接超时的问题。

**根本原因**: 
- 之前的令牌撤销逻辑使用了应用程序级别的API，可能影响整个OAuth应用状态
- GitHub对令牌撤销后的OAuth流程有安全检查机制

**解决方案**:
1. **已优化令牌撤销逻辑**: 使用用户级别的令牌撤销API (`/applications/{client_id}/grant`)
2. **增加HTTP连接优化**: 配置了合理的连接超时和重试机制
3. **备用撤销方案**: 当主要撤销方法失败时，自动尝试备用方案
4. **智能错误处理**: 撤销失败时检查令牌状态，提供用户指导

**临时解决方案** (如果仍遇到问题):
- 访问 [GitHub应用设置](https://github.com/settings/applications) 手动撤销应用授权
- 使用"完整登出"替代"全局登出"，可以清除所有会话但保留令牌有效性
- 检查系统诊断页面 `/debug/token-diagnosis` 查看令牌状态

### API接口
- `GET /sso/logout` - 单点登出选择页面
- `POST /sso/logout` - 执行单点登出
- `POST /sso/api/logout` - API登出接口
- `GET /sso/api/status` - 会话状态查询
- `WS /ws/logout` - WebSocket实时通知

### 调试接口
- `GET /debug` - 系统调试诊断页面
- `GET /debug/session-status` - 获取当前用户会话状态
- `GET /debug/token-diagnosis` - OAuth2令牌状态诊断
- `GET /debug/github-session-status` - GitHub授权状态详细分析
- `GET /debug/register-session` - 手动注册当前会话（调试用）
- `GET /debug/cleanup-sessions` - 清理无效会话（调试用）

## 📚 相关文档

- [环境搭建指南](docs/setup-guide.md)
- [GitHub OAuth配置](docs/github-oauth-setup.md)
- [环境变量配置指南](docs/environment-setup.md)
- [API文档](docs/api-documentation.md)
- [OAuth2登出机制技术文档](student/OAuth2登出机制技术文档.md)
- [真正的单点登出实现技术文档](student/真正的单点登出实现技术文档.md)

## 🔧 开发规范

- 遵循Spring Boot最佳实践
- 代码符合SOLID原则
- 使用UTF-8编码
- 遵循RESTful API设计规范

---

**最后更新时间**: 2025年06月27日 10:50 星期四 
