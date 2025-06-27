# SpringBoot OAuth2 单点登录 - 环境搭建指南

## 📋 前置要求

### 必需软件
- **Java**: JDK 21或更高版本
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Git**: 用于代码管理

### 推荐工具
- **IDE**: IntelliJ IDEA 或 Eclipse
- **数据库管理工具**: MySQL Workbench 或 Navicat
- **API测试工具**: Postman 或 Insomnia

## 🛠️ 环境准备

### 1. 验证Java环境
```bash
java -version
# 应显示 Java 21.x.x

javac -version
# 应显示 javac 21.x.x
```

### 2. 验证Maven环境
```bash
mvn -version
# 应显示 Maven 3.6+ 和 Java 21
```

### 3. 验证MySQL环境
```bash
mysql --version
# 应显示 MySQL 8.0+
```

## 🗄️ 数据库配置

### 1. 启动MySQL服务
```bash
# Windows
net start mysql80

# macOS (Homebrew)
brew services start mysql

# Linux
sudo systemctl start mysql
```

### 2. 创建数据库
```sql
-- 连接到MySQL
mysql -u root -p

-- 创建数据库
CREATE DATABASE OAuth2db
    DEFAULT CHARACTER SET = 'utf8mb4'
    COLLATE = 'utf8mb4_unicode_ci';

-- 验证创建
SHOW DATABASES;
```

### 3. 执行初始化脚本
```bash
# 在项目根目录执行
mysql -u root -p OAuth2db < sql/init.sql
```

## 🔑 GitHub OAuth应用配置

### 1. 创建GitHub OAuth应用
1. 登录GitHub，进入 **Settings** → **Developer settings** → **OAuth Apps**
2. 点击 **New OAuth App**
3. 填写应用信息：
   - **Application name**: `SpringBoot OAuth2 SSO`
   - **Homepage URL**: `http://localhost:8080`
   - **Authorization callback URL**: `http://localhost:8080/login/oauth2/code/github`
4. 点击 **Register application**

### 2. 获取客户端凭据
- 复制 **Client ID**
- 生成并复制 **Client Secret**

### 3. 配置环境变量
创建 `.env` 文件（或设置系统环境变量）：
```bash
# GitHub OAuth2配置
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret

# 数据库配置
DB_PASSWORD=your_mysql_password
```

## 🚀 项目启动

### 1. 克隆项目
```bash
git clone <项目地址>
cd springBoot单点登录\(OAuth2\)
```

### 2. 安装依赖
```bash
cd backend
mvn clean install
```

### 3. 更新配置文件
编辑 `backend/src/main/resources/application.yml`：
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID:your-github-client-id}
            client-secret: ${GITHUB_CLIENT_SECRET:your-github-client-secret}
```

### 4. 启动应用
```bash
# 方式一：使用Maven
mvn spring-boot:run

# 方式二：使用IDE
# 运行 OAuth2SsoApplication.java 主类
```

### 5. 验证启动
- 应用地址: http://localhost:8080
- 健康检查: http://localhost:8080/actuator/health
- 控制台应显示: "Started OAuth2SsoApplication"

## 🔧 开发环境配置

### 1. IDE配置
**IntelliJ IDEA:**
- 安装插件: Lombok, Spring Boot
- 配置Maven: File → Settings → Build Tools → Maven
- 设置JDK: File → Project Structure → Project SDK

**Eclipse:**
- 安装Spring Tools 4插件
- 导入Maven项目: File → Import → Existing Maven Projects

### 2. 代码规范
- 编码格式: UTF-8
- 行尾符: LF
- 缩进: 4个空格
- 最大行长度: 120字符

### 3. 热重载配置
确保 `spring-boot-devtools` 依赖已添加：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

## 🐛 常见问题

### 1. 端口被占用
```bash
# 查找占用端口8080的进程
netstat -ano | findstr :8080  # Windows
lsof -i :8080                 # macOS/Linux

# 杀死进程
taskkill /PID <PID> /F        # Windows
kill -9 <PID>                 # macOS/Linux
```

### 2. MySQL连接失败
- 检查MySQL服务是否启动
- 验证用户名密码
- 确认数据库OAuth2db存在
- 检查防火墙设置

### 3. GitHub OAuth认证失败
- 验证Client ID和Client Secret
- 检查回调URL是否正确
- 确认GitHub应用状态为活跃

### 4. Maven依赖下载慢
配置国内镜像源，编辑 `~/.m2/settings.xml`：
```xml
<mirrors>
    <mirror>
        <id>aliyun</id>
        <name>Aliyun Maven</name>
        <url>https://maven.aliyun.com/repository/public</url>
        <mirrorOf>central</mirrorOf>
    </mirror>
</mirrors>
```

## 📊 监控和调试

### 1. 应用监控
- Actuator端点: http://localhost:8080/actuator
- 健康检查: http://localhost:8080/actuator/health
- 应用信息: http://localhost:8080/actuator/info

### 2. 日志配置
日志文件位置: `logs/oauth2-sso.log`

调整日志级别（application-dev.yml）：
```yaml
logging:
  level:
    com.example.oauth2sso: DEBUG
    org.springframework.security.oauth2: DEBUG
```

### 3. 数据库监控
```sql
-- 查看用户表数据
SELECT * FROM users WHERE delete_flag = '0';

-- 查看最近登录用户
SELECT username, last_login FROM users 
WHERE delete_flag = '0' 
ORDER BY last_login DESC LIMIT 10;
```

## 🚀 生产环境部署

### 1. 构建生产包
```bash
mvn clean package -Pprod
```

### 2. 环境变量配置
```bash
export SPRING_PROFILES_ACTIVE=prod
export GITHUB_CLIENT_ID=prod_client_id
export GITHUB_CLIENT_SECRET=prod_client_secret
export DB_PASSWORD=prod_db_password
```

### 3. 启动应用
```bash
java -jar target/oauth2-sso-1.0.0.jar
```

---

## 📞 技术支持

如果遇到问题，请检查：
1. 本文档的常见问题部分
2. 项目的GitHub Issues
3. Spring Boot官方文档
4. Spring Security OAuth2文档

**作者**: Luowenqiang  
**更新时间**: 2024-12-26 