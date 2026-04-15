# 后台管理系统 Demo

一个基于 `Vue2 + Spring Boot 2.7.x + Spring Security + Spring Cloud Gateway + Redis + MyBatis-Plus + MySQL 8` 的前后端分离后台管理系统 Demo。

本项目重点演示以下企业后台常见能力：

- `gateway` 统一入口、跨域处理、白名单放行、token 基础校验
- `auth` 服务负责登录、登出、当前用户信息、JWT 签发
- `system` 服务负责用户、角色、菜单、部门、日志、Dashboard
- 基于 `RBAC` 的菜单、页面、按钮、接口权限全链路控制
- 前端动态路由 + `v-permission` 指令
- 后端接口二次鉴权，明确区分 `401` / `403`
- Redis 在线 token 管理

## 1. 项目结构

```text
backendManager/
├─ .mvn/                         # 项目级 Maven 配置，修正镜像/TLS 问题
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
└─ README.md
```

## 2. 模块职责

### backend-common

- 统一返回对象 `ApiResult`
- 分页对象 `PageResult`
- 全局异常处理
- JWT 工具
- Redis 工具
- 当前登录人上下文
- 公共实体
- 权限判断 Bean
- 操作日志注解

### backend-gateway

- 统一网关入口
- 白名单配置
- 跨域配置
- JWT 基础校验
- Redis 在线 token 校验
- 向下游透传用户标识

### backend-auth

- 登录
- 登出
- 获取当前用户信息、角色、权限、菜单树
- 修改个人密码
- 登录日志记录

### backend-system

- Dashboard
- 用户管理
- 角色管理
- 菜单管理
- 部门管理
- 登录日志查询
- 操作日志查询

### web-admin

- 登录页
- 主布局页
- 动态菜单与动态路由
- 按钮权限指令
- 各业务页面与表单交互

## 3. 技术栈与版本

### 后端

- JDK 8 目标兼容
- Spring Boot 2.7.18
- Spring Security 5.x
- Spring Cloud 2021.0.8
- Spring Cloud Gateway
- MyBatis-Plus 3.5.5
- MySQL 8
- Redis
- Maven 多模块

### 前端

- Vue 2.6.14
- Vue Router 3
- Vuex 3
- Axios
- Element UI 2.15.x

## 4. 启动前准备

### 4.1 数据库

创建数据库：

```sql
CREATE DATABASE backend_manager DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

执行脚本：

```sql
SOURCE backend/sql/schema.sql;
SOURCE backend/sql/data.sql;
```

### 4.2 Redis

确保本机 Redis 已启动，默认使用：

- host: `127.0.0.1`
- port: `6379`
- db: `0`

### 4.3 修改配置

以下文件里都保留了中文注释，你只需要改成自己的环境值：

- [backend-gateway application.yml](/d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-gateway/src/main/resources/application.yml)
- [backend-auth application.yml](/d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-auth/src/main/resources/application.yml)
- [backend-system application.yml](/d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-system/src/main/resources/application.yml)

重点修改：

- MySQL 地址、用户名、密码
- Redis 地址、端口、密码

## 5. 启动步骤

### 5.1 后端打包

在项目根目录执行：

```bash
mvn -f backend/pom.xml clean package
```

如果你的机器 Maven 全局镜像有 TLS 问题，本项目已自带：

- `.mvn/settings.xml`
- `.mvn/jvm.config`

用于尽量规避镜像与 TLS 协商问题。

### 5.2 启动后端服务

按以下顺序启动：

```bash
mvn -f backend/backend-auth/pom.xml spring-boot:run
mvn -f backend/backend-system/pom.xml spring-boot:run
mvn -f backend/backend-gateway/pom.xml spring-boot:run
```

默认端口：

- gateway: `8080`
- auth: `9200`
- system: `9300`

### 5.3 启动前端

```bash
cd web-admin
npm install
npm run serve
```

默认访问地址：

```text
http://localhost:8081
```

## 6. 默认账号密码

所有默认账号密码统一为：

```text
Admin@123456
```

账号列表：

- `admin`：超级管理员
- `sysadmin`：系统管理员
- `operator`：运营角色
- `auditor`：审计/只读角色
- `devuser`：辅助演示账号

## 7. 权限验证说明

### 7.1 登录后前端会获取什么

登录成功后，前端会调用：

```text
GET /api/auth/user/profile
```

拿到以下数据：

- 当前用户基本信息
- 角色列表
- 权限码集合
- 当前用户可访问菜单树

### 7.2 菜单、路由、按钮如何打通

- 菜单树来自数据库 `sys_menu`
- 页面路由来自后端菜单树中的 `MENU`
- 目录来自后端菜单树中的 `CATALOG`
- 按钮权限来自后端菜单树中的 `BUTTON`
- 前端按钮通过 `v-permission` 控制显示
- 后端接口通过 `@PreAuthorize("@perm.hasPermission(...)")` 做二次校验

### 7.3 重要说明

前端隐藏按钮不等于权限完成。

本项目已经按两层做了权限控制：

1. 前端控制“看不看得到”
2. 后端控制“做不做得了”

也就是说：

- 没有按钮权限时，前端按钮不会显示
- 就算手工构造请求，后端接口也会再次校验权限

### 7.4 401 / 403 区分

- `401`：未登录、token 过期、token 非法、token 不在线
- `403`：已登录，但没有接口权限

前端处理：

- 401 自动跳转登录页
- 403 跳转到 403 页面

## 8. 角色演示差异

### admin

- 拥有全部菜单、页面、按钮权限

### sysadmin

- 拥有大多数系统管理权限
- 可看到用户、角色、菜单、部门、日志、个人中心

### operator

- 只显示工作台、用户管理、部门管理、个人中心
- 不显示角色管理、菜单管理、日志管理

### auditor

- 可看到工作台、用户管理只读、角色管理只读、日志管理、个人中心
- 没有新增、编辑、授权、删除等按钮

## 9. 关键实现说明

### 9.1 网关鉴权

`backend-gateway` 中的 `AuthTokenGlobalFilter` 负责：

- 放行白名单
- 检查 `Authorization` 头
- 解析 JWT
- 判断 token 是否过期
- 到 Redis 检查 token 是否在线
- 失败时返回 401

### 9.2 后端方法级权限校验

`auth/system` 服务都启用了方法级权限控制，例如：

```java
@PreAuthorize("@perm.hasPermission('system:user:list')")
```

### 9.3 前端按钮权限

前端统一使用：

```html
<el-button v-permission="'system:user:add'">新增用户</el-button>
```

### 9.4 动态路由

前端在登录后根据后端返回的菜单树，调用：

- `buildAsyncRoutes`
- `router.addRoute`

动态挂载页面路由。

## 10. UTF-8 编码要求

这是本项目的硬性约束。

所有文件必须使用 `UTF-8` 编码，包括：

- `.java`
- `.yml`
- `.sql`
- `.md`
- `.vue`
- `.js`

本项目已在 Maven 父工程中显式配置：

- `project.build.sourceEncoding=UTF-8`
- `project.reporting.outputEncoding=UTF-8`
- 编译插件 `UTF-8`

### 编辑器建议

请确保以下编辑器都设置为 UTF-8：

- IDEA
- Cursor
- VSCode

如果你在 Windows PowerShell 中查看中文文件，偶尔看到控制台输出乱码，通常是终端显示编码问题，不代表文件本身不是 UTF-8。

本项目源码文件请始终按 UTF-8 保存，不要改成 ANSI 或带 BOM 的异常格式。

## 11. 后续扩展建议

你后续如果要接入 Nacos，可以从以下位置继续演进：

- `backend-gateway` 路由从静态 `uri` 改成服务发现
- `auth/system` 补充 `spring-cloud-starter-alibaba-nacos-discovery`
- 统一配置中心后，把 JWT、Redis、数据库连接放入配置中心

如果要继续增强，还可以补：

- 验证码
- 单点登录
- 强制下线
- token 黑名单
- 菜单缓存
- 字典管理
- 参数审计

## 12. 当前已知说明

本项目在当前工作区是从空目录直接生成的，属于可逐步落地的完整 Demo 工程骨架与核心实现。

如果你接下来需要，我可以继续帮你做两件事：

1. 补后端单元测试和联调测试
2. 帮你继续接入 Nacos 或把 `auth/system` 再拆得更企业化
