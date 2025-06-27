# 环境变量配置指南

## 📋 概述

本项目使用环境变量来管理敏感配置信息，确保安全性并支持不同环境的部署。

## 🔧 必需的环境变量

### 1. 数据库配置

```bash
# MySQL数据库密码
DB_PASSWORD=your_actual_database_password
```

### 2. GitHub OAuth2应用配置

首先需要在GitHub创建OAuth应用：

1. 访问：https://github.com/settings/applications/new
2. 填写应用信息：
   - **应用名称**：SpringBoot OAuth2 SSO Demo
   - **主页URL**：http://localhost:8080
   - **回调URL**：http://localhost:8080/login/oauth2/code/github
3. 创建后获取Client ID和Client Secret

```bash
# GitHub OAuth2 客户端ID
GITHUB_CLIENT_ID=your_actual_github_client_id

# GitHub OAuth2 客户端密钥  
GITHUB_CLIENT_SECRET=your_actual_github_client_secret
```

## 🚀 配置方式

### 方法1：操作系统环境变量

#### Windows
```cmd
set DB_PASSWORD=your_password
set GITHUB_CLIENT_ID=your_client_id
set GITHUB_CLIENT_SECRET=your_client_secret
```

#### macOS/Linux
```bash
export DB_PASSWORD=your_password
export GITHUB_CLIENT_ID=your_client_id
export GITHUB_CLIENT_SECRET=your_client_secret
```

### 方法2：IDE配置

在IDE（如IntelliJ IDEA）的运行配置中设置环境变量：
- Run/Debug Configurations → Environment Variables

### 方法3：创建本地配置文件

创建 `backend/src/main/resources/application-local.yml`：

```yaml
spring:
  datasource:
    password: your_actual_database_password
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: your_actual_github_client_id
            client-secret: your_actual_github_client_secret
```

然后启动时指定profile：
```bash
java -jar app.jar --spring.profiles.active=local
```

## ⚠️ 安全注意事项

1. **永远不要**将真实的敏感信息提交到版本控制系统
2. **生产环境**应使用安全的密钥管理服务
3. **定期轮换**OAuth2客户端密钥
4. **限制**GitHub OAuth应用的权限范围

## 🛠️ 验证配置

启动应用后，访问调试端点验证配置：

```bash
# 检查应用配置状态
curl http://localhost:8080/debug/session-status

# 检查GitHub OAuth2配置
curl http://localhost:8080/debug/github-session-status
```

## 📚 参考文档

- [GitHub OAuth Apps创建指南](../docs/github-oauth-setup.md)
- [Spring Boot外部化配置](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config)

---

**创建时间**: 2025年06月27日
**版本**: 1.0.0 