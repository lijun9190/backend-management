# Backend Manager 请求全流程说明

本文档基于当前仓库代码梳理，从前端点击登录开始，说明请求如何在 `web-admin`、`backend-gateway`、`backend-auth`、`backend-system`、Redis 之间流转。

文档目标：
- 说明“登录态”是如何创建、缓存、校验、透传、失效的
- 说明前端为什么登录后还要拉取 `profile`
- 说明下游服务如何获取当前登录用户
- 说明 gateway 透传的 `X-User-Id` / `X-Username` 当前处于什么角色

## 1. 服务角色

- `web-admin`
  前端 Vue2 管理台，负责登录、保存 token、拉取用户信息、动态生成路由、发起业务请求。

- `backend-gateway`
  所有前端请求的统一入口，负责基础登录态校验，并按路径转发到 `auth` 或 `system` 服务。

- `backend-auth`
  负责登录、登出、当前用户资料、修改个人密码。

- `backend-system`
  负责用户、角色、菜单、部门、日志等后台业务接口。

- `Redis`
  保存 token 对应的登录用户快照，是 gateway 和下游服务共同依赖的登录态存储。

## 2. 前端登录入口

前端登录页在：
- [web-admin/src/views/login/index.vue](d:/Dev/AiCode/Cursor_code/backendManager/web-admin/src/views/login/index.vue)

登录按钮点击后执行：
- `handleLogin()` 调用 `this.$store.dispatch('user/login', this.form)`
- 成功后执行 `this.$router.push(redirect || '/')`

对应 Vuex 逻辑在：
- [web-admin/src/store/modules/user.js](d:/Dev/AiCode/Cursor_code/backendManager/web-admin/src/store/modules/user.js)

关键流程：
1. `user/login` 调用前端 API `login(form)`
2. `login(form)` 请求 `/api/auth/login`
3. 登录成功后，把返回的 `accessToken` 写入 cookie
4. 同时把 token 写入 Vuex `state.user.token`

前端 API 定义在：
- [web-admin/src/api/auth.js](d:/Dev/AiCode/Cursor_code/backendManager/web-admin/src/api/auth.js)

## 3. 前端请求是怎么带上 token 的

统一请求封装在：
- [web-admin/src/utils/request.js](d:/Dev/AiCode/Cursor_code/backendManager/web-admin/src/utils/request.js)

请求拦截器逻辑：
1. 从 cookie 读取 token
2. 如果 token 存在，则写入请求头：
   `Authorization: Bearer <token>`
3. 再把请求发给后端

这意味着：
- 登录后所有业务请求默认都会带 `Authorization`
- 前端并不关心下游到底是 `auth` 还是 `system`
- 前端永远只面向 gateway 的统一地址发请求

## 4. 登录请求进入 gateway

gateway 核心过滤器在：
- [backend/backend-gateway/src/main/java/com/example/gateway/filter/AuthTokenGlobalFilter.java](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-gateway/src/main/java/com/example/gateway/filter/AuthTokenGlobalFilter.java)

gateway 安全配置在：
- [backend/backend-gateway/src/main/java/com/example/gateway/config/SecurityConfig.java](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-gateway/src/main/java/com/example/gateway/config/SecurityConfig.java)

当前设计里，gateway 关闭了 Spring Security 默认拦截，真正的认证逻辑放在 `AuthTokenGlobalFilter` 中统一处理。

### 4.1 登录接口为什么能直接通过 gateway

`AuthTokenGlobalFilter` 会先判断当前路径是否在白名单中。

白名单路径会直接放行，不检查 token。典型白名单包括：
- `/api/auth/login`
- `/error`
- `OPTIONS` 预检请求

因此：
- 登录请求不会被 gateway 要求先带 token
- 未登录用户可以正常访问登录接口

## 5. `/api/auth/login` 的完整链路

### 5.1 前端发起请求

前端发起：
- `POST /api/auth/login`

请求体包含：
- `username`
- `password`

### 5.2 gateway 按路径转发到 auth 服务

gateway 路由规则在配置文件中定义，`/api/auth/**` 会转发到 `backend-auth`。

### 5.3 auth 控制器接收请求

登录控制器在：
- [backend/backend-auth/src/main/java/com/example/auth/controller/AuthController.java](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-auth/src/main/java/com/example/auth/controller/AuthController.java)

入口方法：
- `login(LoginDTO dto, HttpServletRequest request)`

它继续调用：
- `authService.login(dto, request)`

### 5.4 auth 服务校验用户名密码

核心逻辑在：
- [backend/backend-auth/src/main/java/com/example/auth/service/impl/AuthServiceImpl.java:74](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-auth/src/main/java/com/example/auth/service/impl/AuthServiceImpl.java:74)

登录时依次执行：
1. 根据用户名查 `sys_user`
2. 判断用户是否存在
3. 判断是否被逻辑删除
4. 判断账号状态是否禁用
5. 使用 `PasswordEncoder` 校验密码
6. 登录失败时写登录日志
7. 登录成功时构建 `LoginUser`

### 5.5 构建登录用户快照 `LoginUser`

相关代码：
- [AuthServiceImpl.java:149](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-auth/src/main/java/com/example/auth/service/impl/AuthServiceImpl.java:149)
- [backend/backend-common/src/main/java/com/example/common/model/security/LoginUser.java](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-common/src/main/java/com/example/common/model/security/LoginUser.java)

`LoginUser` 不是数据库实体，而是“当前登录用户上下文快照”，包含：
- `userId`
- `deptId`
- `username`
- `nickname`
- `deptName`
- `status`
- `roles`
- `permissions`

其中：
- 角色来自用户与角色关系表
- 权限来自菜单/按钮权限码
- 超级管理员通过 `isSuperAdmin()` 判定

### 5.6 生成 JWT

JWT 工具类在：
- [backend/backend-common/src/main/java/com/example/common/security/JwtTokenProvider.java](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-common/src/main/java/com/example/common/security/JwtTokenProvider.java)

创建 token 的逻辑：
- [JwtTokenProvider.java:26](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-common/src/main/java/com/example/common/security/JwtTokenProvider.java:26)

JWT 当前包含的 claim 很轻量：
- `sub`: userId
- `username`
- `nickname`
- `iat`
- `exp`

注意：
- JWT 本身不包含完整权限列表
- 完整权限放在 Redis 里的 `LoginUser`

### 5.7 登录态写入 Redis

缓存逻辑在：
- [AuthServiceImpl.java:173](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-auth/src/main/java/com/example/auth/service/impl/AuthServiceImpl.java:173)

Redis key 规则在：
- [backend/backend-common/src/main/java/com/example/common/constant/RedisKeyConstants.java](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-common/src/main/java/com/example/common/constant/RedisKeyConstants.java)

当前会写两份数据：

1. `admin:login:token:{token}` -> `LoginUser`
   用途：
   - gateway 检查 token 是否仍然有效
   - auth/system 服务反查当前用户上下文

2. `admin:login:user:{userId}` -> `token`
   用途：
   - 为后续单点登录、强制下线等扩展预留

### 5.8 auth 返回登录结果给前端

返回结构是 `LoginVO`，包含：
- `accessToken`
- `tokenType`，当前是 `Bearer`
- `expireIn`

前端收到后，把 `accessToken` 持久化到 cookie。

## 6. 登录成功后，前端为什么还要请求 `/api/auth/user/profile`

前端在路由守卫里做二次初始化，代码在：
- [web-admin/src/main.js](d:/Dev/AiCode/Cursor_code/backendManager/web-admin/src/main.js)

关键逻辑：
1. 只要前端本地有 token，就认为“可能已登录”
2. 如果动态路由还没加载：
   - 调 `user/fetchProfile`
   - 根据返回的 `menus` 动态生成路由
   - `router.addRoute(...)`
3. 然后再继续当前导航

因此登录不是“拿到 token 就结束”，而是分成两步：

第一步：
- `/api/auth/login`
- 目的：拿 token

第二步：
- `/api/auth/user/profile`
- 目的：拿当前用户资料、角色、权限、菜单树

前端为什么必须拿 `profile`：
- 要知道当前用户昵称、角色、按钮权限
- 要知道当前用户能看到哪些菜单
- 要把后端菜单树转成前端动态路由

动态路由转换逻辑在：
- [web-admin/src/utils/route.js](d:/Dev/AiCode/Cursor_code/backendManager/web-admin/src/utils/route.js)

## 7. `/api/auth/user/profile` 的完整链路

### 7.1 gateway 先校验 token

接口不是白名单，因此会进入 `AuthTokenGlobalFilter` 的鉴权流程：

1. 从 `Authorization` 头里取 token
2. 调 `jwtTokenProvider.validateToken(token)` 验证签名和过期时间
3. 用 `RedisKeyConstants.loginTokenKey(token)` 检查 Redis 是否存在登录态
4. 如果任一步失败，gateway 直接返回 `401`

这一步说明：
- JWT 合法还不够
- Redis 中也必须有对应登录态
- 所以“登出后旧 token 还没过期”也会被判为无效

### 7.2 gateway 透传用户头

校验通过后，gateway 还会附加：
- `X-User-Id`
- `X-Username`

代码位置：
- [AuthTokenGlobalFilter.java:61](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-gateway/src/main/java/com/example/gateway/filter/AuthTokenGlobalFilter.java:61)

重要说明：
- 这两个头当前确实会传给下游
- 但当前 `auth` / `system` 服务并没有依赖它们来完成认证
- 下游现在仍然是自己再次解析 `Authorization`

所以这两个头目前更像“附加上下文透传”，不是认证主依据。

### 7.3 auth 服务再次解析 token

auth 的 JWT 过滤器在：
- [backend/backend-auth/src/main/java/com/example/auth/security/JwtAuthenticationFilter.java](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-auth/src/main/java/com/example/auth/security/JwtAuthenticationFilter.java)

核心逻辑：
1. 从 `Authorization` 读取 token
2. 再次校验 JWT 是否合法
3. 用 token 到 Redis 中取回 `LoginUser`
4. 如果取到：
   - 塞入 `SecurityContextHolder`
   - 塞入 `LoginUserContext`
5. 请求结束后清理上下文

这说明当前项目采用的是：
- gateway 做第一层登录态校验
- 下游服务再做第二层校验和上下文组装

### 7.4 `profile` 控制器获取当前用户

控制器在：
- [backend/backend-auth/src/main/java/com/example/auth/controller/ProfileController.java](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-auth/src/main/java/com/example/auth/controller/ProfileController.java)

接口：
- `GET /api/auth/user/profile`

调用：
- `authService.getCurrentUserProfile()`

实现位置：
- [AuthServiceImpl.java:105](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-auth/src/main/java/com/example/auth/service/impl/AuthServiceImpl.java:105)

### 7.5 auth 服务组装 profile 返回前端

`getCurrentUserProfile()` 会：
1. 从 `LoginUserContext` 取当前登录人
2. 组装基础信息：
   - userId
   - username
   - nickname
   - deptName
   - roles
   - permissions
3. 再查一次 `sys_user` 补充：
   - realName
   - phone
   - email
   - avatar
4. 查询当前用户可访问菜单
5. 构造成菜单树 `menus`
6. 返回 `UserProfileVO`

菜单加载和树构建在：
- [AuthServiceImpl.java:227](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-auth/src/main/java/com/example/auth/service/impl/AuthServiceImpl.java:227)
- [AuthServiceImpl.java:231](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-auth/src/main/java/com/example/auth/service/impl/AuthServiceImpl.java:231)

## 8. 前端拿到 profile 后做了什么

前端 `fetchProfile()` 返回的结果会写入 Vuex：
- `userInfo`
- `roles`
- `permissions`
- `menus`

然后 `permission/generateRoutes` 根据 `menus` 构造动态路由。

这一步完成后：
- 左侧菜单能显示
- 页面路由能命中
- 指令 `v-permission` 可以决定按钮是否显示

也就是说：
- `roles` / `permissions` 主要影响前端展示和按钮控制
- 真正后端接口是否允许访问，还要看后端自己的权限校验

## 9. 普通业务请求示例：访问角色列表 `/api/system/roles/page`

下面以角色页为例说明一条完整业务请求是如何被鉴权的。

### 9.1 前端发起请求

角色页请求定义在：
- [web-admin/src/api/role.js](d:/Dev/AiCode/Cursor_code/backendManager/web-admin/src/api/role.js)

请求：
- `GET /api/system/roles/page`

请求头仍然会带：
- `Authorization: Bearer <token>`

### 9.2 gateway 校验并转发

gateway 再次执行同样流程：
1. 校验 token
2. 检查 Redis 中登录态是否存在
3. 追加 `X-User-Id`、`X-Username`
4. 转发到 `backend-system`

### 9.3 system 服务再次解析 token

system JWT 过滤器在：
- [backend/backend-system/src/main/java/com/example/system/security/JwtAuthenticationFilter.java](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-system/src/main/java/com/example/system/security/JwtAuthenticationFilter.java)

逻辑与 auth 服务同构：
1. 读 `Authorization`
2. 校验 JWT
3. 从 Redis 取 `LoginUser`
4. 写入 `SecurityContextHolder`
5. 写入 `LoginUserContext`
6. 请求结束后清理

### 9.4 Spring Security 判断“有没有登录”

system 安全配置在：
- [backend/backend-system/src/main/java/com/example/system/config/SecurityConfig.java](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-system/src/main/java/com/example/system/config/SecurityConfig.java)

配置特点：
- `SessionCreationPolicy.STATELESS`
- 除 `/error` 外全部接口都要求认证
- 未认证返回 401
- 无权限返回 403

也就是说：
- 先过“已登录”这一关
- 再过“有权限”这一关

### 9.5 角色接口做方法级权限校验

控制器在：
- [backend/backend-system/src/main/java/com/example/system/controller/RoleController.java](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-system/src/main/java/com/example/system/controller/RoleController.java)

角色列表接口：
- [RoleController.java:32](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-system/src/main/java/com/example/system/controller/RoleController.java:32)

它带有：
- `@PreAuthorize("@perm.hasPermission('system:role:list')")`

真正判断逻辑在：
- [backend/backend-common/src/main/java/com/example/common/security/PermissionEvaluatorBean.java](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-common/src/main/java/com/example/common/security/PermissionEvaluatorBean.java)

判断规则：
1. 从 `LoginUserContext` 取当前用户
2. 如果为空，直接无权限
3. 如果是超级管理员，直接放行
4. 否则检查 `permissions` 是否包含 `system:role:list`

### 9.6 业务层如何拿当前登录人

如果接口通过鉴权，业务层可以继续使用：
- `LoginUserContext.get()`

例如角色服务中审计字段回填：
- [backend/backend-system/src/main/java/com/example/system/service/impl/RoleServiceImpl.java:134](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-system/src/main/java/com/example/system/service/impl/RoleServiceImpl.java:134)

这里用当前登录用户的 `username` 回填：
- `createBy`
- `updateBy`

因此 `LoginUserContext` 的作用是：
- 给 Controller 之后的 Service / AOP / 审计代码使用
- 避免每层都手动传 `userId`、`username`

## 10. 登出流程

### 10.1 前端发起登出

前端调用：
- `POST /api/auth/logout`

### 10.2 gateway 先校验 token

因为 `/api/auth/logout` 不是白名单，请求必须带有效 token。

### 10.3 auth 服务删除 Redis 登录态

登出逻辑在：
- [AuthServiceImpl.java:97](d:/Dev/AiCode/Cursor_code/backendManager/backend/backend-auth/src/main/java/com/example/auth/service/impl/AuthServiceImpl.java:97)

当前实现会删除：
- `admin:login:token:{token}`

注意：
- `admin:login:user:{userId}` 当前不会在登出时一起删除
- 当前主校验依赖的是 `loginTokenKey(token)`
- 所以现有功能仍然正常

### 10.4 前端本地清理 token

前端 `user/logout` 会：
1. 调后端 logout
2. 无论后端是否成功，都删除本地 cookie token
3. 重置 Vuex 用户状态

这样前端和后端都会失去登录态。

## 11. 为什么 gateway 和下游都要校验 token

当前项目采用的是“双重校验”：

第一层：gateway
- 统一入口做基础登录态检查
- 能尽早拦截非法请求
- 统一返回 401

第二层：auth / system
- 各自重新解析 `Authorization`
- 自己从 Redis 恢复 `LoginUser`
- 自己构建 `SecurityContext`
- 保持服务自身的安全闭环

这意味着即使：
- 某人绕过 gateway 直接访问下游服务

只要下游暴露端口可访问，它依然会自己检查 token，不会因为“只信 gateway”而裸奔。

## 12. `X-User-Id` / `X-Username` 当前到底有没有用

有透传，但当前不是主流程依赖。

现状：
- gateway 会写这两个头
- 下游当前没有用这两个头做认证
- 下游仍然读 `Authorization` 重新解析 JWT

因此现在的结论是：
- 它们是“附加透传上下文”
- 不是“认证主依据”
- 删掉它们，当前登录与权限主链路仍可工作
- 如果以后希望“只在 gateway 解 token，下游不再重复解”，这两个头可以作为演进基础

## 13. 一条完整请求的最短总结

以“登录后访问角色列表”为例，最短链路如下：

1. 前端登录页调用 `/api/auth/login`
2. `auth` 校验用户名密码
3. 生成 JWT
4. 把 `LoginUser` 写入 Redis，key 为 `admin:login:token:{token}`
5. 前端把 token 写入 cookie
6. 前端进入路由守卫，请求 `/api/auth/user/profile`
7. gateway 校验 token 和 Redis 登录态
8. `auth` 再次解析 token，从 Redis 取回 `LoginUser`
9. `auth` 返回用户资料、权限、菜单树
10. 前端生成动态路由
11. 前端请求 `/api/system/roles/page`
12. gateway 再次校验 token 和 Redis 登录态
13. `system` 再次解析 token，从 Redis 取回 `LoginUser`
14. `@PreAuthorize("@perm.hasPermission('system:role:list')")` 校验权限
15. 业务执行并返回结果

## 14. 核心结论

- 当前真正的登录态中心不是 JWT 本身，而是 Redis 中的 `LoginUser`
- JWT 主要承担“轻量身份凭证”作用
- gateway 和下游都参与了认证，但下游仍然保留独立校验能力
- 前端登录后必须拉 `profile`，因为动态菜单、动态路由、按钮权限都依赖它
- `X-User-Id` / `X-Username` 当前只是附加透传头，不是现有主认证链路的必要条件
