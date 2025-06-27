# 部署指南

## 📋 概述

本指南说明如何将SpringBoot OAuth2单点登录项目推送到GitHub并发布版本。

## 🚀 推送到GitHub

### 1. 推送主分支代码

```bash
# 推送代码到main分支
git push origin main
```

### 2. 推送版本标签

```bash
# 推送v1.0.0标签到远程仓库
git push origin v1.0.0
```

### 3. 验证推送结果

推送成功后，在GitHub仓库页面应该能看到：
- ✅ 完整的项目代码
- ✅ v1.0.0版本标签
- ✅ Release页面显示新版本

## 📦 创建GitHub Release

### 1. 访问Release页面

在GitHub仓库页面点击 "Releases" → "Create a new release"

### 2. 配置Release信息

- **Tag version**: `v1.0.0`
- **Release title**: `SpringBoot OAuth2单点登录系统 v1.0.0`
- **Description**: 复制以下内容

```markdown
# 🎉 SpringBoot OAuth2单点登录系统 v1.0.0

首次发布 - 完整的OAuth2单点登录解决方案

## ✨ 核心特性

- 🔐 **GitHub OAuth2单点登录** - 安全便捷的第三方认证
- 🚪 **真正的单点登出** - 令牌撤销+会话同步+实时通知
- 🌐 **WebSocket实时通知** - 跨设备即时登出通知
- 🔄 **多层级登出模式** - 本地/完整/全局登出选择
- 📱 **跨设备会话管理** - 多浏览器/设备统一管理
- 🛠️ **完整调试诊断工具** - 会话状态、令牌诊断、系统监控

## 🔧 技术栈

- **后端**: Spring Boot 3.x + Spring Security + OAuth2 Client
- **数据库**: MySQL 8.0 + JPA/Hibernate  
- **前端**: Thymeleaf + Bootstrap + WebSocket
- **实时通信**: WebSocket + SockJS
- **构建工具**: Maven
- **开发工具**: JRebel热部署支持

## 📚 完整文档

- 📖 [README - 项目概述](./README.md)
- 🚀 [环境搭建指南](./docs/setup-guide.md)
- 🔧 [环境变量配置](./docs/environment-setup.md)
- 🎯 [GitHub OAuth配置](./docs/github-oauth-setup.md)
- 📊 [API文档](./docs/api-documentation.md)

## 🔒 安全特性

- ✅ 敏感信息环境变量化管理
- ✅ OAuth2令牌安全撤销机制
- ✅ 会话安全控制和过期管理
- ✅ 完善的错误处理和异常保护
- ✅ 网络连接优化和超时配置

## 🛡️ 许可证

本项目基于 [MIT License](./LICENSE) 开源许可证发布。

## 🎯 快速开始

1. **克隆项目**
   ```bash
   git clone https://github.com/Gcluowenqiang/springboot-oauth2sso-demo.git
   cd springboot-oauth2sso-demo
   ```

2. **配置环境变量**
   ```bash
   export DB_PASSWORD=your_database_password
   export GITHUB_CLIENT_ID=your_github_client_id
   export GITHUB_CLIENT_SECRET=your_github_client_secret
   ```

3. **启动应用**
   ```bash
   cd backend
   mvn spring-boot:run
   ```

4. **访问应用**
   - 应用首页: http://localhost:8080
   - 系统诊断: http://localhost:8080/debug

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📞 联系

- 作者: Luowenqiang
- 项目: SpringBoot OAuth2 SSO Demo
- 版本: v1.0.0
```

### 3. 发布Release

点击 "Publish release" 完成版本发布。

## 🔄 后续版本管理

### 版本命名规范

使用语义化版本号：`vMAJOR.MINOR.PATCH`

- **MAJOR**: 不兼容的API修改
- **MINOR**: 向后兼容的功能性新增
- **PATCH**: 向后兼容的问题修正

### 发布新版本流程

1. **开发新功能**
2. **更新版本号** (pom.xml, application.yml)
3. **更新文档**
4. **提交代码**
5. **创建标签**
6. **推送代码和标签**
7. **创建GitHub Release**

## 📊 项目统计

- **文件数量**: 39个
- **代码行数**: 9000+行
- **功能模块**: 10+个
- **文档页面**: 8个
- **技术组件**: 15+个

---

**创建时间**: 2025年06月27日
**版本**: 1.0.0
**状态**: 准备发布 