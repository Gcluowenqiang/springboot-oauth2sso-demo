# GitHub OAuth2 应用配置指南

## 📋 概述

本指南将详细说明如何在GitHub上创建和配置OAuth2应用，以便与SpringBoot OAuth2单点登录项目集成。

## 🔐 OAuth2基础概念

### 什么是OAuth2？
OAuth2是一个授权框架，允许第三方应用获得对用户账户的有限访问权限。在我们的项目中：
- **资源所有者**: GitHub用户
- **客户端**: 我们的SpringBoot应用
- **授权服务器**: GitHub
- **资源服务器**: GitHub API

### 授权流程
1. 用户点击"使用GitHub登录"
2. 重定向到GitHub授权页面
3. 用户授权我们的应用
4. GitHub重定向回我们的应用并携带授权码
5. 我们的应用用授权码换取访问令牌
6. 使用访问令牌获取用户信息

## 🛠️ 创建GitHub OAuth应用

### 步骤1: 登录GitHub
1. 打开 https://github.com
2. 登录你的GitHub账户

### 步骤2: 进入开发者设置
1. 点击右上角头像
2. 选择 **Settings**
3. 在左侧菜单中选择 **Developer settings**
4. 选择 **OAuth Apps**

### 步骤3: 创建新应用
1. 点击 **New OAuth App** 按钮
2. 填写应用信息

## 📝 应用配置详解

### 基本信息配置

#### Application name (应用名称)
```
SpringBoot OAuth2 SSO Demo
```
- 用户在授权时会看到这个名称
- 建议使用描述性的名称

#### Homepage URL (主页URL)
**开发环境:**
```
http://localhost:8080
```

**生产环境:**
```
https://yourdomain.com
```

#### Application description (应用描述)
```
基于SpringBoot的OAuth2单点登录示例应用，演示GitHub OAuth2集成功能。
```

#### Authorization callback URL (授权回调URL)
**开发环境:**
```
http://localhost:8080/login/oauth2/code/github
```

**生产环境:**
```
https://yourdomain.com/login/oauth2/code/github
```

⚠️ **重要提示**: 回调URL必须精确匹配，包括协议、域名、端口和路径。

## 🔑 获取客户端凭据

### 步骤1: 注册应用
点击 **Register application** 完成应用创建。

### 步骤2: 获取Client ID
创建成功后，你将看到 **Client ID**，这是公开的标识符。

### 步骤3: 生成Client Secret
1. 点击 **Generate a new client secret**
2. 复制并安全保存 **Client Secret**
3. ⚠️ **注意**: Client Secret只显示一次，请立即保存

## ⚙️ 应用配置

### 环境变量配置

#### 方式1: 环境变量文件(.env)
```bash
# GitHub OAuth2配置
GITHUB_CLIENT_ID=your_actual_client_id_here
GITHUB_CLIENT_SECRET=your_actual_client_secret_here
```

#### 方式2: 系统环境变量
**Windows:**
```cmd
set GITHUB_CLIENT_ID=your_actual_client_id_here
set GITHUB_CLIENT_SECRET=your_actual_client_secret_here
```

**macOS/Linux:**
```bash
export GITHUB_CLIENT_ID=your_actual_client_id_here
export GITHUB_CLIENT_SECRET=your_actual_client_secret_here
```

### Spring Boot配置
在 `application.yml` 中配置：
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID:your-github-client-id}
            client-secret: ${GITHUB_CLIENT_SECRET:your-github-client-secret}
            scope:
              - user:email
              - read:user
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            client-name: GitHub
        provider:
          github:
            authorization-uri: https://github.com/login/oauth/authorize
            token-uri: https://github.com/login/oauth/access_token
            user-info-uri: https://api.github.com/user
            user-name-attribute: login
```

## 🔒 权限范围说明

### 我们使用的权限
- **user:email**: 访问用户的邮箱地址
- **read:user**: 读取用户的公开个人资料信息

### GitHub权限范围详解
| 范围 | 描述 | 我们是否使用 |
|------|------|--------------|
| `user` | 访问完整的用户资料 | ❌ |
| `user:email` | 访问用户邮箱地址 | ✅ |
| `user:follow` | 关注/取消关注用户 | ❌ |
| `public_repo` | 访问公共仓库 | ❌ |
| `repo` | 访问私有仓库 | ❌ |
| `read:user` | 读取用户资料 | ✅ |

## 🧪 测试配置

### 1. 验证配置
启动应用后访问：
```
http://localhost:8080/login
```

### 2. 检查重定向
点击GitHub登录按钮，应该重定向到：
```
https://github.com/login/oauth/authorize?client_id=YOUR_CLIENT_ID&redirect_uri=...
```

### 3. 授权测试
1. 在GitHub授权页面点击 **Authorize**
2. 应该重定向回你的应用
3. 检查用户信息是否正确显示

## 🚨 安全最佳实践

### 1. Client Secret保护
- ❌ 不要将Client Secret提交到代码仓库
- ✅ 使用环境变量存储敏感信息
- ✅ 在生产环境中使用安全的密钥管理服务

### 2. 回调URL验证
- ✅ 确保回调URL使用HTTPS（生产环境）
- ✅ 验证回调URL的域名是你控制的
- ✅ 不要使用通配符域名

### 3. 权限最小化
- ✅ 只请求应用实际需要的权限
- ✅ 定期审查和更新权限范围
- ✅ 向用户明确说明权限用途

## 🔄 多环境配置

### 开发环境
```yaml
# application-dev.yml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID_DEV}
            client-secret: ${GITHUB_CLIENT_SECRET_DEV}
```

### 生产环境
```yaml
# application-prod.yml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID_PROD}
            client-secret: ${GITHUB_CLIENT_SECRET_PROD}
```

## 🐛 常见问题

### 1. "redirect_uri_mismatch" 错误
**原因**: 回调URL不匹配
**解决方案**: 
- 检查GitHub应用配置中的回调URL
- 确保协议、域名、端口、路径完全一致

### 2. "invalid_client" 错误
**原因**: Client ID或Client Secret错误
**解决方案**:
- 验证环境变量是否正确设置
- 重新生成Client Secret

### 3. 权限不足错误
**原因**: 请求的权限范围不足
**解决方案**:
- 检查scope配置
- 重新授权应用

### 4. 本地开发HTTPS问题
**原因**: GitHub要求生产环境使用HTTPS
**解决方案**:
- 开发环境可以使用HTTP
- 生产环境必须配置HTTPS

## 📊 监控和维护

### 1. 应用使用统计
在GitHub OAuth Apps页面可以查看：
- 授权用户数量
- API调用统计
- 错误率统计

### 2. 安全审计
定期检查：
- 是否有异常的API调用
- 权限范围是否仍然合适
- Client Secret是否需要轮换

### 3. 用户管理
- 用户可以在GitHub Settings → Applications中撤销授权
- 应用应该优雅处理撤销授权的情况

## 📚 参考资料

- [GitHub OAuth2文档](https://docs.github.com/en/developers/apps/building-oauth-apps)
- [Spring Security OAuth2客户端文档](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [OAuth2 RFC规范](https://tools.ietf.org/html/rfc6749)

---

## 📞 技术支持

如果在配置过程中遇到问题：
1. 检查GitHub OAuth Apps的状态
2. 验证回调URL配置
3. 查看应用日志中的OAuth2相关错误
4. 参考Spring Security OAuth2文档

**作者**: Luowenqiang  
**更新时间**: 2024-12-26 