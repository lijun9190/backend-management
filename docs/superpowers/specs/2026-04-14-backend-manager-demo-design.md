# 后台管理系统 Demo 设计文档

## 1. 项目目标

构建一个可运行、接近真实企业后台项目形态的前后端分离后台管理系统 Demo。该 Demo 需要满足统一网关入口、JWT 认证、Redis 在线会话、RBAC 权限控制、菜单路由按钮联动控制、后端接口二次鉴权、日志记录、基础系统管理能力等要求，并保证依赖兼容 JDK 8。

本项目以“先保证能跑通，再兼顾结构清晰和可扩展性”为首要原则，不引入 Nacos 等注册中心，采用静态路由配置降低启动门槛。后续如需服务治理能力，可在当前结构基础上平滑演进。

## 2. 总体架构

项目采用前后端分离架构，所有前端请求统一经过 Gateway 进行入口控制与转发。后端按 Maven 多模块组织，拆分为网关、认证、系统管理和公共模块。

### 2.1 模块划分

- `backend-common`
  - 放置统一返回体、异常体系、JWT 工具、Redis 工具、常量、公共模型、权限判断辅助类、日志注解等公共代码。
- `backend-gateway`
  - 作为统一入口，负责跨域处理、白名单配置、JWT 基础校验、Redis 在线 token 校验、路由转发。
- `backend-auth`
  - 负责登录、登出、当前登录用户信息查询、个人密码修改、JWT 签发、登录日志记录。
- `backend-system`
  - 负责 Dashboard、用户管理、角色管理、菜单管理、部门管理、登录日志查询、操作日志查询。
- `web-admin`
  - 提供后台管理前端页面，负责登录态管理、动态路由注册、菜单渲染、按钮权限指令、业务页面展示与交互。

### 2.2 基础设施

- 数据库：MySQL 8
- 缓存/会话：Redis
- 网关：Spring Cloud Gateway
- 认证授权：Spring Security + JWT
- 持久层：MyBatis-Plus
- 前端：Vue 2 + Vue Router 3 + Vuex 3 + Axios + Element UI 2.x

### 2.3 服务关系

- 浏览器仅访问 `gateway`
- `gateway` 根据静态路由将请求分发给 `auth` 或 `system`
- `auth` 与 `system` 共用一套 MySQL 表结构
- `auth` 和 `system` 共用 Redis，用于在线 token、会话信息及可选缓存

采用 `auth` 与 `system` 共库方案，是为了保证第一版 Demo 更容易启动与联调，同时保留后续拆分数据库的演进空间。

## 3. 技术选型与版本约束

### 3.1 后端版本策略

- JDK：8
- Spring Boot：2.7.x
- Spring Security：5.x（跟随 Boot 2.7.x）
- Spring Cloud：2021.x
- MyBatis-Plus：3.5.x
- JWT：`jjwt 0.9.1`
- Redis：`spring-boot-starter-data-redis`

所有依赖均需选择兼容 JDK 8 的版本，Maven 父工程显式配置 UTF-8 编码，避免中文注释和 SQL 脚本乱码。

### 3.2 前端版本策略

- Vue：2.6.x
- Vue Router：3.x
- Vuex：3.x
- Axios：1.x 或兼容 Vue2 的稳定版本
- Element UI：2.15.x

## 4. 权限模型设计

系统采用 RBAC（基于角色的访问控制）模型，并将资源统一抽象为三类：

- 目录（Catalog）
- 菜单（Menu）
- 按钮（Button）

### 4.1 资源含义

- 目录
  - 用于左侧导航分组和路由层级组织，不直接对应具体页面。
- 菜单
  - 对应一个可访问页面，通常具备 `path`、`component`、`icon`、`visible`、`status` 等字段。
- 按钮
  - 对应某个页面内的具体操作权限，不参与路由生成，仅作为前后端权限点。

### 4.2 权限码规范

权限码统一来自数据库 `sys_menu.permission_code` 字段，命名格式保持一致，例如：

- `system:user:view`
- `system:user:list`
- `system:user:add`
- `system:user:edit`
- `system:user:reset-password`
- `system:role:list`
- `system:role:assign`
- `system:menu:list`
- `system:menu:add`

前端按钮权限指令、动态路由过滤、后端接口权限注解全部使用同一套权限码，避免前后端命名不一致。

### 4.3 超级管理员规则

- 角色编码固定为 `SUPER_ADMIN`
- 超级管理员默认拥有全部菜单和按钮权限
- 超级管理员仍需要通过登录认证，但在细粒度权限判断时直接放行

## 5. 认证与鉴权设计

### 5.1 登录流程

1. 前端提交用户名和密码到 `POST /api/auth/login`
2. `auth-service` 校验账号状态、密码、角色状态
3. 认证成功后签发 JWT
4. 将 token 在线状态和用户会话摘要写入 Redis
5. 记录登录日志
6. 返回 `accessToken`、`tokenType`、`expireIn`
7. 前端拿到 token 后，调用 `GET /api/auth/user/profile`
8. 获取当前用户基本信息、角色列表、权限码集合、菜单树
9. 前端根据菜单树注册动态路由并渲染侧边栏

### 5.2 登出流程

1. 前端调用 `POST /api/auth/logout`
2. 后端删除 Redis 在线 token 或标记该 token 失效
3. 记录退出相关日志
4. 前端清空 token、用户信息、权限集合和动态路由

### 5.3 Gateway 职责边界

`gateway` 只做认证链路中的“基础校验”，不做业务级权限判断：

- 跨域处理
- 路由转发
- 匿名白名单放行
- JWT 解析与签名校验
- token 是否过期判断
- Redis 在线 token 是否存在判断
- 对非法请求返回 401

### 5.4 业务服务鉴权边界

`auth` 和 `system` 负责做精细权限控制：

- 当前用户身份组装
- 权限码集合加载
- `@PreAuthorize` 方法级权限校验
- 对已登录但无权限访问的请求返回 403

### 5.5 401 / 403 语义区分

- 401：未登录、token 缺失、token 非法、token 过期、token 不在线
- 403：已登录但没有当前接口的权限

前端统一处理：

- 401：清空登录态并跳转登录页
- 403：跳转 403 页面或提示无权限

## 6. 数据模型设计

### 6.1 核心表

- `sys_user`
- `sys_role`
- `sys_menu`
- `sys_dept`
- `sys_user_role`
- `sys_role_menu`
- `sys_login_log`
- `sys_operation_log`

### 6.2 通用字段约定

关键业务表统一带有以下通用字段：

- `create_by`
- `create_time`
- `update_by`
- `update_time`
- `deleted`
- `status`
- `remark`

### 6.3 关键关系

- 用户和角色：多对多，对应 `sys_user_role`
- 角色和资源：多对多，对应 `sys_role_menu`
- 部门树：`sys_dept.parent_id`
- 菜单树：`sys_menu.parent_id`
- 用户归属部门：`sys_user.dept_id`

## 7. 菜单、路由、按钮联动设计

### 7.1 后端菜单返回结构

登录成功后，后端返回当前用户完整权限信息，其中菜单树节点至少包含：

- `id`
- `parentId`
- `menuType`
- `name`
- `path`
- `component`
- `icon`
- `permissionCode`
- `visible`
- `status`
- `sort`
- `children`

### 7.2 前端动态路由生成规则

- `CATALOG`
  - 用于嵌套路由和左侧菜单层级
- `MENU`
  - 用于生成实际页面路由
- `BUTTON`
  - 不生成路由，仅进入权限码集合

前端将后端返回的 `component` 字段映射为本地组件，例如：

- `Layout` -> 主布局组件
- `dashboard/index` -> `views/dashboard/index.vue`
- `system/user/index` -> `views/system/user/index.vue`

### 7.3 前端按钮权限控制

前端实现统一的 `v-permission` 指令：

- 有权限：显示按钮
- 无权限：移除按钮节点或隐藏按钮

示例：

```html
<el-button v-permission="'system:user:add'" type="primary">新增用户</el-button>
```

### 7.4 后端接口二次鉴权

后端接口统一使用权限注解做二次校验，例如：

```java
@PreAuthorize("@perm.hasPermission('system:user:list')")
@GetMapping("/page")
public ApiResult<PageResult<UserPageVO>> page(UserQueryDTO query) {
    return ApiResult.success(userService.pageQuery(query));
}
```

这保证了“前端隐藏按钮不等于权限完成”这一硬性要求。

## 8. 业务模块设计

### 8.1 登录页

- 用户名密码登录
- 登录失败提示
- 默认账号提示
- 登录中状态反馈

### 8.2 Dashboard

- 用户总数
- 角色总数
- 菜单总数
- 最近登录次数
- 最近登录日志列表
- 快捷入口

### 8.3 用户管理

- 分页查询
- 按用户名、昵称、状态筛选
- 新增用户
- 编辑用户
- 启用/禁用用户
- 重置密码
- 分配角色

### 8.4 角色管理

- 分页查询
- 新增角色
- 编辑角色
- 启用/禁用角色
- 分配菜单权限和按钮权限

### 8.5 菜单管理

- 目录/菜单/按钮三种类型
- 树形展示
- 编辑 `path`、`component`、`icon`、`sort`、`permissionCode`、`visible`、`status`

### 8.6 部门管理

- 树形结构
- 新增部门
- 编辑部门
- 删除部门
- 用户归属部门

### 8.7 个人中心

- 查看个人信息
- 修改密码

### 8.8 日志模块

- 登录日志
  - 记录登录时间、IP、浏览器、登录状态、消息
- 操作日志
  - 记录操作模块、请求方法、操作人、耗时、结果

## 9. 后端代码结构设计

### 9.1 Maven 模块结构

```text
backend/
├─ pom.xml
├─ backend-common
├─ backend-gateway
├─ backend-auth
├─ backend-system
└─ sql
```

### 9.2 分层约定

业务服务采用以下分层：

- `controller`
- `service`
- `service.impl`
- `mapper`
- `entity`
- `dto`
- `vo`
- `config`
- `security`
- `common`

### 9.3 公共能力

`backend-common` 提供以下公共能力：

- 统一返回体 `ApiResult`
- 分页响应 `PageResult`
- 业务异常 `BusinessException`
- 全局异常处理
- JWT 工具类
- Redis Key 常量
- 当前登录用户上下文
- 操作日志注解和切面
- 权限判断工具 Bean

## 10. 前端结构设计

### 10.1 页面结构

- 登录页
- 主布局页（侧边栏 + 顶栏 + 面包屑）
- Dashboard
- 用户管理
- 角色管理
- 菜单管理
- 部门管理
- 个人中心
- 登录日志
- 操作日志
- 403 页面
- 404 页面

### 10.2 页面交互要求

各业务页面尽量贴近真实后台系统，统一具备以下元素中的合理组合：

- 查询区
- 工具栏
- 表格区
- 分页
- 弹窗表单
- 状态开关
- 树形控件
- 空状态
- 成功/失败消息反馈

### 10.3 前端状态管理

`Vuex` 中至少维护以下状态：

- token
- 用户信息
- 角色列表
- 权限码集合
- 动态路由加载状态
- 侧边栏菜单数据

## 11. 接口规划

### 11.1 认证接口

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/user/profile`
- `PUT /api/auth/user/password`

### 11.2 Dashboard 接口

- `GET /api/system/dashboard/overview`

### 11.3 用户接口

- `GET /api/system/users/page`
- `GET /api/system/users/{id}`
- `POST /api/system/users`
- `PUT /api/system/users/{id}`
- `PUT /api/system/users/{id}/status`
- `PUT /api/system/users/{id}/reset-password`
- `PUT /api/system/users/{id}/roles`

### 11.4 角色接口

- `GET /api/system/roles/page`
- `POST /api/system/roles`
- `PUT /api/system/roles/{id}`
- `PUT /api/system/roles/{id}/status`
- `PUT /api/system/roles/{id}/menus`
- `GET /api/system/roles/options`
- `GET /api/system/roles/{id}/menu-ids`

### 11.5 菜单接口

- `GET /api/system/menus/tree`
- `GET /api/system/menus/{id}`
- `POST /api/system/menus`
- `PUT /api/system/menus/{id}`
- `DELETE /api/system/menus/{id}`

### 11.6 部门接口

- `GET /api/system/depts/tree`
- `POST /api/system/depts`
- `PUT /api/system/depts/{id}`
- `DELETE /api/system/depts/{id}`

### 11.7 日志接口

- `GET /api/system/login-logs/page`
- `GET /api/system/operation-logs/page`

## 12. Redis 设计

Redis 在本项目中承担在线会话和 token 管理职责。

### 12.1 Key 规划

- `admin:login:token:{token}`
  - 保存 token 在线状态或用户摘要信息
- `admin:login:user:{userId}`
  - 保存用户最近登录 token，可用于后续扩展单点登录或踢下线

### 12.2 当前阶段用途

- 校验 token 是否在线
- 登出时删除 token
- 为后续扩展“强制下线”“单点登录”“权限缓存刷新”预留结构

## 13. 日志设计

### 13.1 登录日志

登录成功和失败都要记录：

- 用户名
- 登录时间
- 登录 IP
- 浏览器/系统信息（可做简化）
- 登录状态
- 提示信息

### 13.2 操作日志

对新增、编辑、状态变更、分配权限、重置密码等关键操作记录：

- 模块名称
- 操作类型
- 请求路径
- 请求方法
- 操作人
- 请求参数摘要
- 执行结果
- 执行耗时
- 操作时间

## 14. 初始化数据设计

初始化数据需覆盖多角色、多用户、多菜单、多按钮权限场景，确保不同角色登录效果明显不同。

### 14.1 初始化角色

- 超级管理员
- 系统管理员
- 运营角色
- 审计/只读角色

### 14.2 初始化用户

至少提供多名用户，并分配不同角色：

- `admin`：超级管理员
- `sysadmin`：系统管理员
- `operator`：运营角色
- `auditor`：审计/只读角色
- 可补充普通员工用户用于部门演示

### 14.3 初始化菜单

需包含完整菜单树：

- 首页 Dashboard
- 系统管理目录
  - 用户管理
  - 角色管理
  - 菜单管理
  - 部门管理
- 日志管理目录
  - 登录日志
  - 操作日志
- 个人中心

每个页面下需补充按钮权限资源，用于权限树分配和前端按钮控制。

## 15. 编码与 UTF-8 要求

本项目所有文件必须统一使用 UTF-8 编码，包括：

- `.java`
- `.xml`
- `.yml`
- `.yaml`
- `.sql`
- `.md`
- `.vue`
- `.js`

同时要求：

- Maven 显式设置 `project.build.sourceEncoding=UTF-8`
- Maven 显式设置 `project.reporting.outputEncoding=UTF-8`
- 编译插件指定 UTF-8
- MySQL 连接串包含 UTF-8 相关参数
- Redis、数据库等配置文件的关键可修改项添加中文注释
- 关键类、关键方法、关键配置保留中文注释，且注释必须可正常显示

README 中需补充说明 IDEA、Cursor、VSCode 应统一设置 UTF-8。

## 16. 运行与交付要求

交付物需至少包括：

- 完整项目目录结构
- Maven 多模块后端代码
- Vue2 前端代码
- `schema.sql`
- `data.sql`
- `README.md`
- 默认账号密码
- 启动步骤
- 权限验证说明

## 17. 实现策略与取舍

为了保证“先跑通，再逐步增强”，第一版按以下策略落地：

- 不引入注册中心
- Gateway 使用静态路由
- 引入 Redis 做 token 在线校验
- `auth` 与 `system` 共用数据库
- 采用 MyBatis-Plus 简化 CRUD
- 前端使用动态路由和按钮权限指令打通完整权限链路

## 18. 风险与边界

### 18.1 当前版本明确包含

- 完整登录鉴权链路
- 角色菜单按钮联动
- 动态路由
- 后端接口权限校验
- 401/403 区分
- 登录日志与操作日志
- 基础管理页面和交互

### 18.2 当前版本暂不包含

- 验证码
- 文件上传
- 多租户
- 国际化
- 注册中心
- 分布式事务
- 复杂审计追踪

上述能力不影响该 Demo 成为一个可运行且贴近真实项目的后台管理系统基础版本。
