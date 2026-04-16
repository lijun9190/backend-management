# Backend Manager 认证与请求流转详解

本文档不是接口清单，而是“把整套登录生命周期讲明白”的说明文档。

目标：

- 解释这套双 token + session 的真实职责分工
- 解释登录、访问业务接口、刷新 token、登出、踢下线分别发生了什么
- 解释 Redis 里 3 类 key 的作用
- 解释为什么双 token 仍然需要 session
- 解释一些容易误解的问题，比如：
  - 为什么 access token 没过期也能刷新出新的 access token
  - 为什么踢下线后还能再次登录
  - 为什么 refresh token 用字符串而不是 JWT

当前仓库采用的是“会话中心化”的认证方案：

- `access token` 负责访问接口
- `refresh token` 负责换发新 token
- `session` 负责服务端登录态治理

这 3 个概念必须分开理解，否则看代码时很容易混淆。

---

## 1. 先说结论：谁才是真正的登录态

这套系统里，真正说了算的不是某个 token，而是 Redis 里的 `session`。

可以先记住这个判断：

- `access token` 没过期，不代表服务端一定还认它
- `refresh token` 还在客户端手里，不代表它一定还能刷新
- 只有服务端 Redis 里那条 `session` 还有效，当前登录才算真正有效

所以：

- `access token` 是“访问凭证”
- `refresh token` 是“续签凭证”
- `session` 是“服务端承认的这次登录记录”

如果你更喜欢现实比喻，可以这么记：

- `access token`：门禁卡
- `refresh token`：续卡凭证
- `session`：物业系统里的住户登记记录

真正说了算的是物业系统里的那条登记记录，不是你手里的卡本身。

---

## 2. 为什么要从单 token 升级到双 token + session

旧思路通常是：

- 登录后发一个 JWT
- 每次请求带这个 JWT
- 服务端验签、验过期，或者顺便去 Redis 查一下在线态

这种方案在“只要能登录访问接口”时够用，但一旦有下面这些需求，就会越来越别扭：

- 单端在线
- access token 自动刷新
- refresh token 轮换
- 刷新后旧 access token 尽快失效
- 强制踢人下线
- 续签不能无限期进行，要有绝对上限

为什么会别扭？

因为如果你让一个 token 同时承担下面 3 件事，就会很难管理：

- 它是访问凭证
- 它是在线态标识
- 它还是服务端治理对象

所以当前实现把职责拆开了：

- `access token`
  - 短有效期
  - 只负责访问接口
- `refresh token`
  - 只负责换发新 token
  - 每次刷新都轮换
- `session`
  - 负责判断当前这次登录是否仍被服务端承认
  - 负责单端在线、踢下线、登出、版本控制、续签边界

---

## 3. 核心概念逐个解释

### 3.1 access token

`access token` 仍然是 JWT，由 `JwtTokenProvider` 生成。

代码位置：

- `backend/backend-common/src/main/java/com/example/common/security/JwtTokenProvider.java`

当前 access token 的主要特点：

- 类型：JWT
- 有效期：默认 `1800s`，即 30 分钟
- 用途：访问业务接口
- 传输方式：前端从 cookie 读取后，放进请求头

请求头格式：

```http
Authorization: Bearer <accessToken>
```

当前 access token 里最关键的 claim：

- `sub`
  - 当前登录用户 ID
- `username`
  - 用户名
- `nickname`
  - 昵称
- `sid`
  - 当前 sessionId
- `typ`
  - token 类型，当前为 `access`
- `av`
  - accessTokenVersion

注意：

- access token 是“可以被解析的”
- 但它不是“最终裁决者”
- 服务端仍然要根据 `sid` 去 Redis 查 session 才能决定是否放行

### 3.2 refresh token

`refresh token` 不是 JWT，而是高强度随机字符串。

生成位置：

- `backend/backend-common/src/main/java/com/example/common/security/LoginSessionManager.java`

当前设计选择字符串而不是 JWT，原因很直接：

- refresh token 不需要被 gateway、system 这些服务直接解析
- refresh token 只给 `auth` 服务使用
- 用字符串 + 服务端哈希存储，更适合做轮换和作废

当前 refresh token 的特点：

- 类型：随机字符串
- 用途：调用 `/api/auth/refresh` 获取新双 token
- 前端存储：cookie
- 传输方式：不是放到 `Authorization`，而是作为刷新接口的请求体字段传给后端

请求体示例：

```json
{
  "refreshToken": "xxxx"
}
```

### 3.3 session

`session` 是当前方案最核心的对象，对应 Java 类：

- `backend/backend-common/src/main/java/com/example/common/model/security/LoginSession.java`

里面保存的是服务端要管理的一整条登录记录：

- `sessionId`
- `userId`
- `loginUser`
- `refreshTokenHash`
- `accessTokenVersion`
- `refreshExpireAt`
- `sessionExpireAt`
- `lastRefreshAt`

这条记录回答的是：

- 当前这次登录是谁
- 当前这次登录还能不能继续访问
- 当前 refresh token 是否还是最新那个
- 当前 access token 的有效版本是多少
- 当前 session 到什么时候必须彻底结束

一句话总结：

`session` 才是“这次登录”的本体。

### 3.4 accessTokenVersion

这个字段非常重要，它的作用是：

- 让旧 access token 在刷新后立即失效

为什么要有它？

因为 JWT 一旦签发，只看 `exp` 的话，在过期前它理论上都还能被拿来访问接口。

但我们现在希望做到：

- 刷新成功后，旧 access token 尽快不可用

所以当前方案会在 refresh 时：

1. 把 session 里的 `accessTokenVersion` 加一
2. 签发一个新的 access token，里面的 `av` 也用新的版本号
3. 以后所有请求都要比较：
   - JWT 里的 `av`
   - Redis session 里的 `accessTokenVersion`

只有两边一致才算有效。

这意味着：

- 旧 JWT 就算还没到过期时间
- 只要版本号落后了
- 也会被判定为无效

### 3.5 LoginUser

`LoginUser` 是登录用户快照，不是数据库表本身。

它大致包含：

- userId
- deptId
- username
- nickname
- deptName
- status
- roles
- permissions

它的作用是：

- 登录成功后，把当前用户的角色与权限快照保存到 session 中
- 下游服务在鉴权通过后，直接从 session 拿到登录上下文

也就是说，业务接口并不是每次都重新去拼装完整权限模型，而是优先使用 session 中的 `loginUser`

---

## 4. Redis 里为什么是 3 类 key

当前登录态相关 Redis key 一共有 3 类。

### 4.1 `admin:login:user:{userId}` -> `sessionId`

作用：

- 记录这个用户当前唯一有效的 session 是谁

它主要服务于：

- 单端在线
- 按用户踢下线
- 第二次登录时挤掉第一次登录

可以把它理解成：

```text
用户 -> 当前在线会话
```

### 4.2 `admin:login:session:{sessionId}` -> `LoginSession`

作用：

- 存 session 主记录

这是整套方案最核心的 key。

它主要服务于：

- 鉴权
- 取当前登录用户信息
- 校验 access token 版本
- 校验 refresh token 是否仍属于当前 session
- 续签时间控制

可以把它理解成：

```text
会话编号 -> 这次登录的完整档案
```

### 4.3 `admin:login:refresh:{refreshTokenHash}` -> `sessionId`

作用：

- 根据 refresh token 的哈希反查它属于哪个 session

它主要服务于：

- `/api/auth/refresh`
- refresh token rotation
- 旧 refresh token 立即失效

可以把它理解成：

```text
refresh token -> 对应哪次登录
```

### 4.4 这 3 类 key 是怎么配合的

登录时：

- 写入 `user -> session`
- 写入 `session -> LoginSession`
- 写入 `refreshHash -> session`

访问接口时：

- access token 里有 `sid`
- 根据 `sid` 查 `session -> LoginSession`
- 再顺便检查 `user -> session` 是否仍指向它

刷新时：

- 先根据 `refreshHash -> session` 找到 session
- 再更新 session 主记录
- 再删除旧的 refresh 索引，写入新的 refresh 索引

踢下线时：

- 先从 `user -> session` 找到当前 sessionId
- 再删 session 主记录
- 再删对应 refresh 索引
- 再删 user -> session

---

## 5. 参与流程的服务各做什么

### 5.1 web-admin

职责：

- 登录后保存 `accessToken` 和 `refreshToken`
- 业务请求带 `Authorization`
- 路由初始化时拉取 `profile`
- 收到 `401` 时尝试 refresh
- refresh 失败时清理本地状态并跳回登录页

关键文件：

- `web-admin/src/views/login/index.vue`
- `web-admin/src/store/modules/user.js`
- `web-admin/src/utils/auth.js`
- `web-admin/src/utils/request.js`
- `web-admin/src/main.js`

### 5.2 backend-gateway

职责：

- 统一入口
- 白名单放行
- 校验 access token 是否有效
- 根据 session 做第一层登录态校验
- 透传基础用户头给下游

关键文件：

- `backend/backend-gateway/src/main/java/com/example/gateway/filter/AuthTokenGlobalFilter.java`

### 5.3 backend-auth

职责：

- 登录
- 刷新 token
- 登出
- 返回当前用户 profile

关键文件：

- `backend/backend-auth/src/main/java/com/example/auth/controller/AuthController.java`
- `backend/backend-auth/src/main/java/com/example/auth/service/impl/AuthServiceImpl.java`

### 5.4 backend-system

职责：

- 处理后台业务接口
- 支持管理员按用户强制踢下线

关键文件：

- `backend/backend-system/src/main/java/com/example/system/controller/UserController.java`
- `backend/backend-system/src/main/java/com/example/system/service/impl/UserServiceImpl.java`

---

## 6. 登录流程：从输入账号密码到拿到双 token

这一节按实际执行顺序来讲。

### 6.1 前端提交登录表单

入口：

- `web-admin/src/views/login/index.vue`

点击登录后，前端会执行：

1. `this.$store.dispatch('user/login', form)`
2. Vuex 调用 `POST /api/auth/login`

### 6.2 auth 校验用户名密码

入口：

- `AuthController.login()`
- `AuthServiceImpl.login()`

`AuthServiceImpl.login()` 里大致会做这些事：

1. 根据用户名查用户
2. 判断用户是否存在
3. 判断是否被逻辑删除
4. 判断账号是否被禁用
5. 校验密码
6. 组装 `LoginUser`
7. 调 `loginSessionManager.createSession(loginUser)`

如果用户没有角色，也不会让登录成功，因为系统依赖 profile 里的角色和菜单来构建前端路由。

### 6.3 createSession 做了什么

真正的登录态创建在：

- `LoginSessionManager.createSession()`

它会按这个顺序执行：

1. 先让该用户已有的旧 session 失效
   - 这一步就是“单端在线”的入口
2. 生成新的 `sessionId`
3. 生成新的 `refreshToken`
4. 对 refresh token 做哈希
5. 创建 `LoginSession`
6. 把 `accessTokenVersion` 初始化为 `1`
7. 计算：
   - `refreshExpireAt`
   - `sessionExpireAt`
8. 写入 3 类 Redis key
9. 签发新的 access token
10. 返回 `AuthTokens`

### 6.4 返回给前端的登录结果

返回对象是：

- `LoginVO`

包含：

- `accessToken`
- `refreshToken`
- `tokenType`
- `accessExpireIn`
- `refreshExpireIn`

### 6.5 前端收到登录结果后做什么

位置：

- `web-admin/src/store/modules/user.js`

当前处理逻辑：

1. 把 `accessToken` 和 `refreshToken` 写到 cookie
2. 把它们同步写到 Vuex
3. 跳转到目标页或首页

注意：

- 这时只是“拿到了双 token”
- 还不算前端初始化完全完成

### 6.6 为什么登录后还要再调一次 profile

位置：

- `web-admin/src/main.js`

前端路由守卫会在发现有 token、但动态路由还没加载时，执行：

1. `user/fetchProfile`
2. 根据返回的 `menus` 生成动态路由
3. `router.addRoute(...)`
4. 再继续当前导航

也就是说，前端完整进入系统内部，其实分成两步：

1. `/api/auth/login`
   - 拿双 token
2. `/api/auth/user/profile`
   - 拿用户资料、角色、权限、菜单

---

## 7. 正常访问业务接口时发生了什么

### 7.1 前端怎么发请求

位置：

- `web-admin/src/utils/request.js`

请求拦截器会：

1. 从 cookie 读取最新 `accessToken`
2. 如果存在，写入：

```http
Authorization: Bearer <accessToken>
```

### 7.2 gateway 怎么校验

位置：

- `AuthTokenGlobalFilter`

gateway 对非白名单请求会做这些检查：

1. 从 `Authorization` 里解析 token
2. 校验它是不是合法 access token
3. 从 JWT 中拿到 `sid`
4. 根据 `sid` 到 Redis 读取 session
5. 判断 session 是否仍在线
6. 比较 JWT 中的 `av` 和 session 中的 `accessTokenVersion`
7. 再确认 `userId -> sessionId` 索引仍指向当前 session

全部通过才放行。

如果失败，则直接返回 `401`。

### 7.3 下游服务为什么还要再验一次

当前实现不是只有 gateway 验，`auth` 和 `system` 的 JWT filter 也会再验一次 session。

这样做的好处是：

- 防止绕过 gateway 直连下游时失去保护
- 下游服务可以直接恢复 `LoginUserContext`

因此：

- gateway 是第一层登录态校验
- auth / system 是第二层登录态校验和上下文恢复

### 7.4 当前登录用户上下文怎么进入业务层

校验通过后，下游会把 `LoginUser` 放入：

- `SecurityContextHolder`
- `LoginUserContext`

这样业务层和 `@PreAuthorize` 都能正常工作。

---

## 8. access token 刷新流程

这一节是最容易混淆的地方。

### 8.1 先记一个核心结论

`access token` 没过期，也完全可以刷新出新的 `access token`。

因为 refresh 看的是：

- session 是否还有效
- refresh token 是否还有效

而不是看旧 access token 是否已经到 `exp`

### 8.2 前端什么时候触发刷新

位置：

- `web-admin/src/utils/request.js`

当前前端策略是：

当一个业务请求返回 `401` 时，如果满足下面条件，就先尝试 refresh：

1. 当前请求不是 `/api/auth/refresh`
2. 当前请求还没重试过
3. 本地还有 refresh token

然后执行：

1. `POST /api/auth/refresh`
2. 成功则保存新的双 token
3. 重试原始请求

### 8.3 refresh 接口在服务端做了什么

入口：

- `AuthController.refresh()`
- `AuthServiceImpl.refreshToken()`
- `LoginSessionManager.refreshSession()`

流程如下：

1. 接收 refresh token
2. 对 refresh token 做哈希
3. 用 `admin:login:refresh:{hash}` 找到 `sessionId`
4. 再读取 `admin:login:session:{sessionId}`
5. 校验 session 是否仍在线
6. 校验 refresh token 哈希是否与 session 当前记录一致
7. 校验：
   - `refreshExpireAt` 未过期
   - `sessionExpireAt` 未过期
8. 生成新的 refresh token
9. 删除旧 refresh 索引
10. 更新 session 中的：
    - `refreshTokenHash`
    - `accessTokenVersion`
    - `refreshExpireAt`
    - `lastRefreshAt`
11. 再生成新的 access token
12. 返回新的双 token

### 8.4 为什么 refresh 后旧 access token 会失效

因为 refresh 的时候：

- session 中的 `accessTokenVersion` 会加一

例如：

1. 登录时：
   - session.version = 1
   - accessToken.av = 1
2. 刷新后：
   - session.version = 2
   - newAccessToken.av = 2
3. 旧 access token 再来请求时：
   - token.av = 1
   - session.version = 2
   - 不一致，直接判失效

所以旧 access token 即使还没到 JWT 过期时间，也会因为版本落后而被拒绝。

### 8.5 为什么 refresh token 也会失效

因为当前实现启用了 refresh token rotation。

意思是：

- 每次 refresh 都会换发一个新的 refresh token
- 旧 refresh token 的索引立即删除

所以旧 refresh token 无法重复使用。

---

## 9. 续签策略：为什么不是无限续命

当前项目不是“给一个 refresh token 然后永远能续”，而是“滑动续签 + 绝对上限”。

### 9.1 access token

- 默认 30 分钟
- 只负责访问接口
- 不自己续命

### 9.2 refresh token

- 默认单次刷新窗口 7 天
- 每次 refresh 都会换发新的 refresh token

### 9.3 session 绝对上限

- 默认 30 天
- 即使用户一直活跃刷新
- 整个 session 到了这个绝对上限也必须重新登录

这套设计比“一个 refresh token 永久可用”更接近真实项目，也更安全。

---

## 10. 单端在线是怎么实现的

你可以把单端在线理解成：

- 一个用户同一时刻只允许 Redis 里存在一个被承认的当前 session

### 10.1 第二次登录时会发生什么

当同一账号再次登录时：

1. `createSession()` 先调用 `invalidateUserSession(userId)`
2. 找到旧 sessionId
3. 删除旧 session 主记录
4. 删除旧 refresh 索引
5. 删除 `user -> session`
6. 再创建新的 session 和新双 token

### 10.2 旧端会表现成什么样

旧端之后再访问接口时：

- 旧 access token 里的 `sid` 对应的 session 已不存在
- gateway / 下游会返回 `401`

旧端再尝试 refresh 时：

- 旧 refresh token 的索引也已经被删除
- refresh 也会失败

最终前端就会清理本地状态并跳回登录页。

这就是“单端在线是在服务端强制成立”的意思。

---

## 11. 登出流程

### 11.1 主动登出

前端调用：

- `POST /api/auth/logout`

后端处理：

1. 从 `Authorization` 里解析 access token
2. 取出 `sid`
3. 使对应 session 失效
4. 删除：
   - session 主记录
   - refresh 索引
   - user -> session

前端处理：

1. 清 access token
2. 清 refresh token
3. 清 Vuex 状态
4. 清动态路由状态

### 11.2 被动登出

如果 session 已失效，比如：

- 被单端在线挤掉
- 被管理员踢下线
- refresh 已过期
- session 到了绝对上限

那么前端在请求过程中会收到 `401`，随后：

1. 尝试 refresh
2. refresh 失败
3. 执行 `user/clearSession`
4. 跳回 `/login`

当前 `request.js` 里还有一层专门处理：

- 当 `401` 已经被系统接管为“提示 + 重定向登录”后
- 当前请求链不再继续向页面抛出错误
- 避免页面闪出额外的错误层

---

## 12. 强制踢人下线流程

### 12.1 前端入口在哪里

入口在：

- 用户管理页面的操作列

文件：

- `web-admin/src/views/system/user/index.vue`

点击“踢下线”后，会调用：

- `DELETE /api/system/users/{id}/session`

### 12.2 后端接口在哪里

文件：

- `backend/backend-system/src/main/java/com/example/system/controller/UserController.java`
- `backend/backend-system/src/main/java/com/example/system/service/impl/UserServiceImpl.java`

权限码：

- `system:user:kickout`

### 12.3 后端具体做了什么

`UserServiceImpl.kickout(id)` 里会：

1. 校验目标用户是否存在
2. 防止当前登录管理员把自己踢下线
3. 调 `loginSessionManager.invalidateUserSession(id)`
4. 写操作日志

`invalidateUserSession(id)` 最终会把这个用户当前唯一在线 session 彻底作废。

### 12.4 被踢用户端会看到什么

被踢用户不会立刻收到服务端推送。

它是在“下一次请求”时感知到自己已失效：

1. 业务请求返回 `401`
2. 前端尝试 refresh
3. refresh 也失败
4. 前端提示“登录状态已失效，请重新登录”
5. 清理本地状态并跳回登录页

### 12.5 为什么踢下线后还能重新登录

这是正常设计。

因为“踢下线”处理的是会话层，不是账号层。

它的意思是：

- 立即废掉你当前这次登录

但它不是：

- 禁用账号
- 封号
- 永久禁止再次登录

如果账号本身还是启用状态，那么重新输入账号密码，仍然可以再次登录。

---

## 13. 前端 token 的存储与传输方式

这一点也很容易误解。

当前前端把两个 token 都保存在 cookie 里，但传给后端的方式不同。

### 13.1 access token

存储位置：

- cookie：`ADMIN_DEMO_ACCESS_TOKEN`

传输方式：

- 前端从 cookie 里读出来
- 写到 `Authorization` 请求头

不是靠浏览器自动附带 cookie 认证。

### 13.2 refresh token

存储位置：

- cookie：`ADMIN_DEMO_REFRESH_TOKEN`

传输方式：

- 平时业务请求不带它
- 只有调 `/api/auth/refresh` 时
- 才会把它放进 request body

---

## 14. 结合代码再看一次完整时序

这里用一条最完整的链路串起来。

### 14.1 用户第一次登录

1. 前端 `POST /api/auth/login`
2. auth 校验账号密码
3. auth 组装 `LoginUser`
4. sessionManager 创建 session
5. Redis 写入：
   - `user -> session`
   - `session -> LoginSession`
   - `refreshHash -> session`
6. 生成 access token
7. 返回双 token
8. 前端写 cookie 和 Vuex
9. 前端再调 `/api/auth/user/profile`
10. 拉取角色、权限、菜单并生成动态路由

### 14.2 用户正常访问业务接口

1. 前端从 cookie 取 access token
2. 放进 `Authorization`
3. gateway 校验 JWT
4. gateway 用 `sid` 查 session
5. 校验版本号
6. 校验 user -> session 是否仍指向它
7. 放行到下游
8. 下游恢复 `LoginUserContext`
9. 业务接口执行

### 14.3 用户 refresh 一次

1. 前端调用 `/api/auth/refresh`
2. auth 根据 refresh token 哈希查到 session
3. 校验 session 仍在线
4. 校验 refresh 和 session 都未过期
5. 生成新的 refresh token
6. 删除旧 refresh 索引
7. session.accessTokenVersion + 1
8. 生成新的 access token
9. 返回新双 token
10. 前端覆盖本地旧双 token

### 14.4 管理员踢下线

1. 管理员调用 `DELETE /api/system/users/{id}/session`
2. system 找到该用户当前 session
3. 删除 session 主记录
4. 删除 refresh 索引
5. 删除 user -> session
6. 被踢用户下一次请求时收到 `401`
7. refresh 也失败
8. 被动退回登录页

---

## 15. 常见疑问

### 15.1 单 token 时代为什么感觉没有 session

因为旧方案里，token 往往同时承担了：

- 访问凭证
- 在线态索引
- 服务端登录态标识

看起来像“没有 session”，其实只是“token 自己兼任了 session 的角色”。

双 token 时代为了支持刷新、版本控制、单端在线、踢下线，才把 session 正式独立出来。

### 15.2 双 token 一定要有 session 吗

严格来说，双 token 概念本身不要求必须有 session。

但如果你要：

- 单端在线
- 旧 token 立即失效
- 踢下线
- refresh rotation
- 续签绝对上限

那么服务端必须有一份“当前承认哪次登录”的状态。

你可以叫它 session，也可以叫 onlineRecord、deviceRecord、tokenFamily，本质是同一种东西。

### 15.3 access token 没过期，为什么还能刷新出新的 access token

因为 refresh 的判断依据不是“旧 access token 是否过期”，而是：

- refresh token 是否有效
- session 是否有效

只要 session 和 refresh token 都还被服务端承认，就可以换发新的 access token。

### 15.4 踢下线后为什么还能再次登录

因为踢下线处理的是会话层，不是账号层。

它只表示：

- 废掉你当前这次登录

但只要账号本身仍是启用状态，重新输入账号密码仍可以再次登录。

### 15.5 refresh token 为什么不用 JWT

因为当前 refresh token 只给 auth 服务使用，不需要被 gateway、system 解析。

用随机字符串的好处是：

- 更方便 rotation
- 更方便立即作废
- 服务端只需要保存哈希

### 15.6 为什么要有 `accessTokenVersion`

因为单靠 JWT 的过期时间，旧 access token 在过期前理论上仍可被使用。

版本号机制可以让：

- 刷新后的旧 access token
- 被顶掉旧登录的 access token

在“还没过期”的情况下也立即失效。

---

## 16. 关键代码索引

前端：

- 登录页：`web-admin/src/views/login/index.vue`
- 用户会话状态：`web-admin/src/store/modules/user.js`
- token 存取：`web-admin/src/utils/auth.js`
- 请求拦截与自动刷新：`web-admin/src/utils/request.js`
- 路由守卫：`web-admin/src/main.js`
- 用户管理踢下线按钮：`web-admin/src/views/system/user/index.vue`

后端：

- auth 控制器：`backend/backend-auth/src/main/java/com/example/auth/controller/AuthController.java`
- auth 服务：`backend/backend-auth/src/main/java/com/example/auth/service/impl/AuthServiceImpl.java`
- session 核心：`backend/backend-common/src/main/java/com/example/common/security/LoginSessionManager.java`
- JWT 生成与解析：`backend/backend-common/src/main/java/com/example/common/security/JwtTokenProvider.java`
- session 模型：`backend/backend-common/src/main/java/com/example/common/model/security/LoginSession.java`
- gateway 校验：`backend/backend-gateway/src/main/java/com/example/gateway/filter/AuthTokenGlobalFilter.java`
- system 踢下线：`backend/backend-system/src/main/java/com/example/system/controller/UserController.java`
- system 踢下线实现：`backend/backend-system/src/main/java/com/example/system/service/impl/UserServiceImpl.java`

---

## 17. 一句话总括

当前项目已经不是“靠一个 token 证明自己在线”，而是：

- 用 `access token` 证明“我想访问接口”
- 用 `refresh token` 证明“我想续签”
- 用 `session` 证明“服务端仍然承认这是当前有效登录”

双 token、单端在线、自动刷新、refresh rotation、续签上限、管理员踢下线，全部都是围绕同一个 `sessionId` 在运转。
