# 后台管理系统 Demo

一个基于 `Vue2 + Spring Boot 2.7.x + Spring Security + Spring Cloud Gateway + Redis + MyBatis-Plus + MySQL 8` 的前后端分离后台管理系统。

当前版本重点演示两件事：

- 一套完整可跑的 RBAC 后台
- 一套更接近真实项目的双 token 会话治理

## 1. 当前能力

- `gateway` 统一入口、白名单放行、access token 会话校验
- `auth` 服务负责登录、刷新 token、登出、当前用户信息
- `system` 服务负责用户、角色、菜单、部门、日志、Dashboard
- 菜单、页面、按钮、接口四层 RBAC
- 单端在线
- `access token` 自动刷新
- `refresh token rotation`
- 续签绝对上限
- 管理员手动强制踢用户下线

## 2. 认证方案

当前不是演示型“表面双 token”，而是会话中心化方案：

- `access token`
  - JWT
  - 短有效期
  - 给 `gateway/auth/system` 鉴权使用
- `refresh token`
  - 随机字符串
  - 只给 `auth` 服务刷新接口使用
  - 每次刷新都会轮换
- `session`
  - Redis 中的真正登录态
  - 通过 `sessionId` 管理单端在线、刷新、登出、踢下线

### 2.1 Redis 关键结构

- `admin:login:user:{userId}` -> 当前唯一 `sessionId`
- `admin:login:session:{sessionId}` -> 会话主记录
- `admin:login:refresh:{refreshTokenHash}` -> refresh token 索引

### 2.2 续签策略

- `access token` 默认 `1800s`
- `refresh token` 默认单次窗口 `7 天`
- session 绝对续签上限默认 `30 天`
- 刷新成功后：
  - 新 access token 生效
  - 旧 access token 因版本号不匹配立即失效
  - 旧 refresh token 立即失效

## 3. 项目结构

```text
backendManager/
├─ backend/
│  ├─ pom.xml
│  ├─ backend-common/
│  ├─ backend-gateway/
│  ├─ backend-auth/
│  ├─ backend-system/
│  └─ sql/
│     ├─ schema.sql
│     └─ data.sql
├─ web-admin/
├─ Logic.md
└─ README.md
```

## 4. 模块职责

### backend-common

- 统一返回对象 `ApiResult`
- 全局异常处理
- JWT access token 工具
- Redis 工具
- `LoginSessionManager`
- 登录用户上下文
- 权限判断 Bean

### backend-gateway

- 所有请求统一入口
- 白名单放行
- 校验 access token 与 Redis session 是否匹配
- 向下游透传 `X-User-Id` / `X-Username`

### backend-auth

- 登录
- refresh token 刷新
- 登出
- 当前用户资料
- 个人密码修改
- 登录日志记录

### backend-system

- Dashboard
- 用户、角色、菜单、部门管理
- 登录日志、操作日志查询
- 管理员强制踢用户下线

### web-admin

- 登录页
- 主布局
- 动态菜单与动态路由
- `v-permission`
- 401 后自动刷新 access token

## 5. 环境准备

### 5.1 数据库

创建数据库：

```sql
CREATE DATABASE `backend-management`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;
```

执行脚本：

```sql
SOURCE backend/sql/schema.sql;
SOURCE backend/sql/data.sql;
```

`data.sql` 已包含 `system:user:kickout` 权限与演示账号数据。

### 5.2 Redis

确保 Redis 可用，默认配置：

- host: `192.168.150.199`
- port: `6379`
- password: `imooc`
- db: `0`

如需本地运行，请按你的环境修改以下文件：

- `backend/backend-gateway/src/main/resources/application.yml`
- `backend/backend-auth/src/main/resources/application.yml`
- `backend/backend-system/src/main/resources/application.yml`

### 5.3 关键认证配置

当前默认值：

```yaml
security:
  jwt:
    secret: backend-manager-secret-key
    access-expire-seconds: 1800
    refresh-expire-seconds: 604800
    refresh-max-expire-seconds: 2592000
```

其中：

- `access-expire-seconds` 只在 `auth` 签发 access token 时使用
- `gateway/system` 也需要相同 `secret` 才能验签
- `refresh` 与 `max` 仅 `auth` 服务使用

## 6. 启动方式

### 6.1 后端

在仓库根目录执行：

```bash
mvn -f backend/pom.xml clean package
```

然后分别启动：

```bash
mvn -f backend/backend-gateway/pom.xml spring-boot:run
mvn -f backend/backend-auth/pom.xml spring-boot:run
mvn -f backend/backend-system/pom.xml spring-boot:run
```

默认端口：

- gateway: `8080`
- auth: `9200`
- system: `9300`

### 6.2 前端

```bash
cd web-admin
npm install
npm run serve
```

开发端口默认：

- web-admin: `8081`

## 7. 默认账号

默认密码统一为：

```text
Admin@123456
```

可用账号：

- `admin`
- `sysadmin`
- `operator`
- `auditor`

## 8. 关键接口

### 认证相关

- `POST /api/auth/login`
  - 登录，返回双 token
- `POST /api/auth/refresh`
  - 用 refresh token 换发新双 token
- `POST /api/auth/logout`
  - 作废当前 session
- `GET /api/auth/user/profile`
  - 获取当前登录用户资料、权限、菜单
- `PUT /api/auth/user/password`
  - 修改本人密码

### 用户会话治理

- `DELETE /api/system/users/{id}/session`
  - 管理员强制踢指定用户当前会话下线
  - 权限码：`system:user:kickout`

## 9. 当前行为说明

### 单端在线

同一账号再次登录时：

- 旧 session 立即失效
- 旧 access token 立即不可用
- 旧 refresh token 也无法继续刷新

### 自动刷新

前端业务请求收到 `401` 时：

1. 如果本地仍有 refresh token，则先调用 `/api/auth/refresh`
2. 刷新成功后自动重放原请求
3. 如果 refresh 失败，则清空本地状态并跳回登录页

### 强制踢下线

管理员在用户管理页点击“踢下线”后：

1. `system` 服务作废该用户当前 session
2. 被踢用户下次请求会收到 `401`
3. 前端尝试 refresh 也会失败
4. 最终回到登录页

## 10. 当前边界

这次实现的是基础版会话治理，当前不会自动把以下动作作为强制下线触发器：

- 禁用账号
- 管理员重置别人密码
- 用户自己修改密码

如果后续需要，可以进一步升级成“状态变更即清 session”的版本。

## 11. 验证

本次改造已验证：

### 后端

```bash
mvn -f backend/pom.xml "-Dtest=AuthServiceLoginRoleGuardTest,ApplicationYamlConfigTest,GatewaySecurityConfigTest,UserServiceDeleteTest,LoginUserRedisSerializationTest,JwtTokenProviderTest,LoginSessionManagerTest" test
```

### 前端

```bash
node web-admin/tests/login-navigation.test.js
node web-admin/tests/login-failure-no-overlay.test.js
node web-admin/tests/request-401-dedup.test.js
node web-admin/tests/request-refresh-retry.test.js
node web-admin/tests/main-empty-menu-guard.test.js
cd web-admin && npm run build
```

前端构建通过，只有 vendor 包体积告警，没有阻塞错误。

## 12. 补充说明

更详细的请求链路、登录态创建、刷新、续签与踢下线说明，请看：

- `Logic.md`
