# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

基于 Vue2 + Spring Boot 2.7.x + Spring Security + Spring Cloud Gateway + Redis + MyBatis-Plus + MySQL 8 的前后端分离后台管理系统，实现完整的 RBAC 权限控制和双 token 会话治理。

## 技术栈

**后端**: Java 8, Spring Boot 2.7.18, Spring Cloud 2021.0.8, MyBatis-Plus 3.5.5, JJWT 0.12.5, Hutool 5.8.28
**前端**: Vue 2.6, Vue Router 3.x, Vuex 3.x, Element UI 2.15, Axios
**基础设施**: MySQL 8, Redis, Maven

## 模块结构

```
backend/
├── backend-common/    # 公共模块：JWT工具、Redis操作、登录会话管理、统一返回对象、全局异常处理
├── backend-gateway/   # 网关服务 (8080)：请求入口、白名单放行、token校验、用户信息透传
├── backend-auth/      # 认证服务 (9200)：登录、刷新token、登出、用户资料、密码修改
├── backend-system/    # 系统服务 (9300)：用户/角色/菜单/部门管理、日志查询、强制踢下线
└── sql/               # 数据库脚本：schema.sql、data.sql
web-admin/             # 前端管理界面 (8081)
```

## 常用命令

### 后端构建与启动

```bash
# 构建整个后端
mvn -f backend/pom.xml clean package

# 单独启动各服务
mvn -f backend/backend-gateway/pom.xml spring-boot:run
mvn -f backend/backend-auth/pom.xml spring-boot:run
mvn -f backend/backend-system/pom.xml spring-boot:run
```

### 后端测试

```bash
# 运行全部测试
mvn -f backend/pom.xml test

# 运行指定测试类
mvn -f backend/pom.xml "-Dtest=AuthServiceLoginRoleGuardTest,ApplicationYamlConfigTest,GatewaySecurityConfigTest,UserServiceDeleteTest,LoginUserRedisSerializationTest,JwtTokenProviderTest,LoginSessionManagerTest" test
```

### 前端开发

```bash
cd web-admin
npm install
npm run serve      # 开发服务器 (8081)
npm run build      # 生产构建
```

### 前端测试

```bash
# 运行单个测试文件
node web-admin/tests/login-navigation.test.js

# 运行所有测试
node web-admin/tests/login-failure-no-overlay.test.js
node web-admin/tests/request-401-dedup.test.js
node web-admin/tests/request-refresh-retry.test.js
node web-admin/tests/main-empty-menu-guard.test.js
```

## 核心架构：双 Token + Session 认证方案

本项目采用会话中心化的认证方案，详细说明请参考 `Logic.md`。

### 认证流程

1. **登录**: Auth 服务验证账号密码，创建 Redis Session，返回 access token (JWT) + refresh token (随机字符串)
2. **访问接口**: Gateway 校验 access token 有效性及 Redis session 状态，透传 X-User-Id/X-Username 到下游服务
3. **刷新 token**: Auth 服务验证 refresh token，轮换生成新双 token，旧 token 立即失效
4. **登出/踢下线**: 作废 Redis session，所有关联 token 立即失效

### Redis 关键结构

- `admin:login:user:{userId}` → 当前唯一 sessionId (单端在线)
- `admin:login:session:{sessionId}` → 会话主记录 (LoginSession 对象)
- `admin:login:refresh:{refreshTokenHash}` → refresh token 索引

### Token 策略

- access token: JWT，默认 1800s (30分钟)
- refresh token: 随机字符串，默认 7 天窗口
- session 绝对续签上限: 默认 30 天

## 代码规范

### 注释要求

所有新增的类、方法必须添加中文注释说明用途。关键业务逻辑、复杂判断、异常处理等位置必须添加必要的中文注释。

### Git 提交信息

必须使用中文，格式示例：
- 功能：新增用户登录接口
- 修复：解决订单重复提交问题
- 优化：调整权限校验逻辑

## 关键配置

### 数据库

```sql
CREATE DATABASE `backend-management` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
SOURCE backend/sql/schema.sql;
SOURCE backend/sql/data.sql;
```

### 应用配置

各服务配置文件位置：
- `backend/backend-gateway/src/main/resources/application.yml`
- `backend/backend-auth/src/main/resources/application.yml`
- `backend/backend-system/src/main/resources/application.yml`

需要根据本地环境修改 Redis 连接信息。

### JWT 密钥

项目使用 RS256 非对称加密，密钥文件路径通过配置指定：
- `security.jwt.private-key-path` (私钥，仅 auth 服务)
- `security.jwt.public-key-path` (公钥，gateway/system 服务)

## 默认账号

密码统一为: `Admin@123456`
可用账号: `admin`, `sysadmin`, `operator`, `auditor`

## 关键接口

### 认证相关
- `POST /api/auth/login` - 登录
- `POST /api/auth/refresh` - 刷新 token
- `POST /api/auth/logout` - 登出
- `GET /api/auth/user/profile` - 获取用户资料、权限、菜单
- `PUT /api/auth/user/password` - 修改密码

### 会话治理
- `DELETE /api/system/users/{id}/session` - 管理员强制踢用户下线 (权限码: `system:user:kickout`)

## 权限控制

采用菜单、页面、按钮、接口四层 RBAC 模型：
- 后端使用 `@PreAuthorize("@perm.hasPermission('xxx')")` 进行方法级鉴权
- 前端使用 `v-permission` 指令控制按钮显隐
- 后端返回 `roles/permissions/menus`，前端根据 `menus` 生成动态路由

## 重要文档

- `Logic.md` - 认证与请求流转详解，解释双 token + session 的完整工作原理
- `项目.md` - 后端认证鉴权方