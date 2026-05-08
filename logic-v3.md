# Backend Manager 认证与会话逻辑详解 v3

本文档基于当前仓库代码重新整理，目标不是重复接口清单，而是把这套认证体系背后的“裁决规则、状态迁移、失效条件、排障路径”讲清楚。

如果只想记一个结论：

> 当前项目不是纯 JWT 登录。JWT 只是访问凭证，真正决定用户是否在线的是 Redis 里的 `LoginSession`。

这意味着很多看似反直觉的现象都是正常结果：

- `access token` 还没过期，也可能被拒绝
- `refresh token` 还在浏览器里，也可能不能刷新
- 同账号第二次登录，会把第一次登录顶掉
- 管理员踢下线不会封号，只会废掉当前会话
- 修改密码、重置密码、禁用用户，按当前实现不会自动清理已有会话

---

## 1. 当前认证模型

当前实现可以概括为：

```text
JWT access token + opaque refresh token + Redis server-side session
```

三个对象的职责不能混在一起：

| 对象 | 类型 | 主要用途 | 最终裁决权 |
| --- | --- | --- | --- |
| `access token` | JWT | 访问业务接口 | 没有 |
| `refresh token` | 随机字符串 | 换发新双 token | 没有 |
| `LoginSession` | Redis 中的服务端记录 | 判断这次登录是否仍被承认 | 有 |

所以当前系统的真实登录态不是“浏览器里有没有 token”，而是：

```text
Redis 中是否存在当前 session，并且 user/session/refresh 三类索引仍互相一致。
```

---

## 2. 为什么不是纯 JWT

纯 JWT 的核心特点是“服务端不保存会话状态，只验签和过期时间”。这种方式简单，但很难做好下面这些能力：

- 单账号单活跃会话
- 主动登出后立即失效
- 管理员强制踢人下线
- refresh token rotation
- refresh 后旧 access token 立即失效
- 会话续签存在绝对上限

当前项目需要这些能力，所以 JWT 不能是唯一依据。JWT 里只放当前请求所需的身份线索，例如用户 ID、`sessionId`、版本号；服务端仍然要回 Redis 查 session，才能最终决定放不放行。

---

## 3. 核心不变量

理解这套实现，优先看不变量，而不是先看接口。

### 3.1 一个用户最多只有一个当前有效 session

Redis 中只有一条：

```text
admin:login:user:{userId} -> sessionId
```

它表达的是：

```text
这个用户当前被服务端承认的会话是谁。
```

同一用户再次登录时，`createSession()` 会先调用 `invalidateUserSession(userId)`，回收旧会话，再创建新会话。

### 3.2 一个 session 有且只有一个当前 refresh token

`LoginSession.refreshTokenHash` 保存当前有效 refresh token 的哈希。

每次 refresh 成功后：

- 旧 refresh 索引被删除
- 新 refresh token 被生成
- session 中的 `refreshTokenHash` 被替换
- 新 refresh 索引写入 Redis

所以旧 refresh token 不能重复使用。

### 3.3 一个 session 有一个当前 access token 版本

`LoginSession.accessTokenVersion` 是 access token 的服务端版本号。

JWT 里也有一个 `av` claim。

业务请求要通过，必须满足：

```text
JWT.av == LoginSession.accessTokenVersion
```

因此 refresh 之后，旧 access token 即使还没自然过期，也会因为版本落后而失效。

### 3.4 session 有两个时间边界

当前实现区分：

- `refreshExpireAt`：当前 refresh token 的单次刷新窗口
- `sessionExpireAt`：这次 session 的绝对上限

典型默认值：

- access token：`1800s`
- refresh token 单次窗口：`604800s`
- session 绝对上限：`2592000s`

这不是无限续签。只要 `sessionExpireAt` 到了，即使用户一直活跃，也必须重新登录。

---

## 4. 关键代码索引

### 4.1 认证核心

- `backend/backend-common/src/main/java/com/example/common/security/LoginSessionManager.java`
- `backend/backend-common/src/main/java/com/example/common/security/JwtTokenProvider.java`
- `backend/backend-common/src/main/java/com/example/common/model/security/LoginSession.java`
- `backend/backend-common/src/main/java/com/example/common/model/security/LoginUser.java`
- `backend/backend-common/src/main/java/com/example/common/constant/RedisKeyConstants.java`

### 4.2 后端入口

- `backend/backend-auth/src/main/java/com/example/auth/controller/AuthController.java`
- `backend/backend-auth/src/main/java/com/example/auth/controller/ProfileController.java`
- `backend/backend-auth/src/main/java/com/example/auth/service/impl/AuthServiceImpl.java`
- `backend/backend-gateway/src/main/java/com/example/gateway/filter/AuthTokenGlobalFilter.java`
- `backend/backend-auth/src/main/java/com/example/auth/security/JwtAuthenticationFilter.java`
- `backend/backend-system/src/main/java/com/example/system/security/JwtAuthenticationFilter.java`

### 4.3 用户会话治理

- `backend/backend-system/src/main/java/com/example/system/controller/UserController.java`
- `backend/backend-system/src/main/java/com/example/system/service/impl/UserServiceImpl.java`

### 4.4 前端会话协作

- `web-admin/src/views/login/index.vue`
- `web-admin/src/store/modules/user.js`
- `web-admin/src/utils/auth.js`
- `web-admin/src/utils/request.js`
- `web-admin/src/main.js`
- `web-admin/src/views/system/user/index.vue`

---

## 5. `access token`

`access token` 由 `JwtTokenProvider.createAccessToken()` 签发。

它是 JWT，前端访问业务接口时放在请求头：

```http
Authorization: Bearer <accessToken>
```

当前 access token 里关键 claim：

| claim | 含义 |
| --- | --- |
| `sub` | 用户 ID |
| `username` | 用户名 |
| `nickname` | 昵称 |
| `sid` | 当前 session ID |
| `typ` | token 类型，当前为 `access` |
| `av` | access token 版本号 |

注意两点：

1. JWT 验签成功只说明“这个 token 是系统签发的，且结构上有效”。
2. 它是否还能访问接口，还要看 Redis 中的 session 是否承认它。

也就是说，`access token` 是入口凭证，不是登录态本身。

---

## 6. `refresh token`

`refresh token` 在 `LoginSessionManager` 中生成。

它不是 JWT，而是随机字符串。后端不会把原始 refresh token 存到 Redis，而是存它的哈希。

当前传输方式：

```json
{
  "refreshToken": "xxxx"
}
```

它只在 `/api/auth/refresh` 中使用，不放进 `Authorization` 请求头。

用随机字符串而不是 JWT 的原因很明确：

- refresh token 不需要被 gateway 或 system 解析
- 它只服务于 auth 的续签流程
- 随机串配合哈希索引，更容易轮换和立即作废

---

## 7. `LoginSession`

`LoginSession` 是这套系统的核心对象。

它保存的是“这次登录”的服务端事实：

| 字段 | 含义 |
| --- | --- |
| `sessionId` | 本次登录的唯一编号 |
| `userId` | 登录用户 ID |
| `loginUser` | 登录用户快照 |
| `refreshTokenHash` | 当前有效 refresh token 的哈希 |
| `accessTokenVersion` | 当前有效 access token 版本 |
| `refreshExpireAt` | 当前 refresh token 过期时间 |
| `sessionExpireAt` | session 绝对过期时间 |
| `lastRefreshAt` | 最近一次刷新时间 |

它回答的问题包括：

- 当前请求属于哪次登录
- 这次登录是否仍在线
- 当前 access token 版本是否还有效
- 当前 refresh token 是否还是最新那个
- 这次会话是否已经超过刷新窗口或绝对上限

所以更准确地说：

```text
LoginSession 才是登录态主体，token 只是访问它或续签它的凭证。
```

---

## 8. Redis 三类 key

当前登录体系依赖三类 Redis key。

### 8.1 `admin:login:user:{userId}`

```text
admin:login:user:{userId} -> sessionId
```

用途：

- 查某个用户当前在线 session
- 实现单账号单活跃会话
- 管理员按用户踢下线
- 同账号再次登录时回收旧会话

如果这条映射不再指向当前 session，说明当前 session 不再是该用户被承认的活跃会话。

### 8.2 `admin:login:session:{sessionId}`

```text
admin:login:session:{sessionId} -> LoginSession
```

用途：

- 保存完整登录记录
- 鉴权时恢复登录用户上下文
- 校验 access token 版本
- 校验 refresh token 哈希
- 控制刷新窗口和绝对过期时间

这是登录态主记录。它没了，通常就意味着这次登录已经结束。

### 8.3 `admin:login:refresh:{refreshTokenHash}`

```text
admin:login:refresh:{refreshTokenHash} -> sessionId
```

用途：

- refresh 接口根据 refresh token 反查 session
- 支持 refresh token rotation
- 让旧 refresh token 立即不可用

如果这条索引没了，客户端手里的 refresh token 就不能再换新 token。

### 8.4 三类 key 的组合关系

一次有效登录通常满足：

```text
user:{userId}              -> sessionId
session:{sessionId}        -> LoginSession(userId, refreshTokenHash, version...)
refresh:{refreshTokenHash} -> sessionId
```

访问业务接口主要走：

```text
access token.sid -> session:{sid} -> LoginSession
```

refresh 主要走：

```text
refresh token -> hash -> refresh:{hash} -> sessionId -> session:{sessionId}
```

踢下线和二次登录主要走：

```text
user:{userId} -> sessionId -> invalidateSession(sessionId)
```

---

## 9. 登录流程

### 9.1 前端提交

入口：

- `web-admin/src/views/login/index.vue`
- `web-admin/src/store/modules/user.js`
- `web-admin/src/api/auth.js`

前端提交账号密码后调用：

```text
POST /api/auth/login
```

gateway 对登录接口白名单放行。

### 9.2 auth 校验

入口：

- `AuthController.login()`
- `AuthServiceImpl.login()`

主要步骤：

1. 按用户名查用户
2. 判断用户是否存在
3. 判断是否逻辑删除
4. 判断账号是否禁用
5. 校验密码
6. 构建 `LoginUser`
7. 调用 `LoginSessionManager.createSession(loginUser)`
8. 记录登录日志

如果用户没有角色，当前实现不会让登录继续完成，因为前端依赖角色、权限、菜单生成动态路由。

### 9.3 `createSession()` 的真实语义

`createSession()` 不是单纯“新增一条 session”。它的语义是：

```text
为这个用户创建一个新的唯一活跃会话，并先回收旧的活跃会话。
```

执行顺序：

1. `invalidateUserSession(userId)`
2. 生成新的 `sessionId`
3. 生成新的 `refreshToken`
4. 计算 `refreshTokenHash`
5. 组装 `LoginSession`
6. 初始化 `accessTokenVersion = 1`
7. 计算 `refreshExpireAt`
8. 计算 `sessionExpireAt`
9. `persistSession(session, refreshTokenHash)`
10. 签发 access token
11. 返回双 token

第 1 步决定了单端在线。

### 9.4 第一次登录

第一次登录时，`invalidateUserSession(userId)` 通常查不到旧 session。

状态变化：

```text
登录前:
user:{userId}              不存在
session:*                  不存在
refresh:*                  不存在

登录后:
user:{userId}              -> newSessionId
session:{newSessionId}     -> new LoginSession
refresh:{newRefreshHash}   -> newSessionId
```

前端收到响应后保存：

- `accessToken`
- `refreshToken`

然后路由守卫会触发 profile 拉取：

```text
GET /api/auth/user/profile
```

profile 返回用户资料、角色、权限、菜单，前端据此生成动态路由。

### 9.5 第二次登录

同一账号第二次登录时，`invalidateUserSession(userId)` 会读到旧 `sessionId`。

状态变化：

```text
第二次登录前:
user:{userId}              -> oldSessionId
session:{oldSessionId}     -> old LoginSession
refresh:{oldHash}          -> oldSessionId

先回收旧会话:
user:{userId}              删除
session:{oldSessionId}     删除
refresh:{oldHash}          删除

再创建新会话:
user:{userId}              -> newSessionId
session:{newSessionId}     -> new LoginSession
refresh:{newHash}          -> newSessionId
```

因此，第二次登录不是复用旧 session，而是：

```text
先销毁旧登录，再创建新登录。
```

旧浏览器不会立即收到推送通知。它会在下一次请求时收到 401，然后 refresh 也失败，最终回到登录页。

---

## 10. 正常访问业务接口

### 10.1 前端发请求

`web-admin/src/utils/request.js` 的请求拦截器会从 cookie 取 `accessToken`，写入：

```http
Authorization: Bearer <accessToken>
```

注意：虽然 token 存在 cookie 中，但业务接口不是靠浏览器自动带 cookie 完成认证，而是前端主动写请求头。

### 10.2 gateway 第一层校验

入口：

- `AuthTokenGlobalFilter`

非白名单请求会做：

1. 解析 `Authorization`
2. 提取 Bearer token
3. 调用 `loginSessionManager.getValidSession(token)`
4. 失败则直接返回 401
5. 成功则向下游透传基础用户信息

### 10.3 `getValidSession()` 的判断链

这个方法是业务请求的关键裁决点。

它不是只验 JWT，而是组合判断：

1. access token 是否合法
2. access token 是否过期
3. token 中是否能取到 `sid`
4. Redis 中是否存在 `session:{sid}`
5. session 是否仍在线
6. JWT 中的 `av` 是否等于 session 中的 `accessTokenVersion`
7. JWT 中的 `userId` 是否等于 session 中的 `userId`

任何一步失败，当前请求都不应被当作已登录请求。

### 10.4 下游服务为什么还要再验一次

auth 和 system 服务也有自己的 JWT 过滤器。

这样做有两个作用：

- 避免绕过 gateway 直连下游时完全失守
- 在下游恢复 `SecurityContextHolder` 和 `LoginUserContext`

所以当前链路是：

```text
gateway 做入口拦截
auth/system 做服务内上下文恢复和二次保护
```

---

## 11. 自动 refresh 流程

### 11.1 前端什么时候 refresh

前端不是定时刷新，而是在业务请求返回 401 后尝试刷新。

`request.js` 中的条件大致是：

1. 当前响应是 401
2. 当前请求不是 refresh 接口
3. 当前请求还没有被重试过
4. 当前请求没有显式跳过自动 refresh
5. 本地仍有 refresh token

满足后：

```text
POST /api/auth/refresh
```

refresh 成功后，前端保存新双 token，并重放原请求。

### 11.2 前端的 `refreshPromise`

同一个 SPA 实例内，多个请求同时 401 时，前端用共享的 `refreshPromise` 去合并 refresh。

效果是：

- 第一个 401 触发 refresh
- 后续 401 等同一个 refresh 结果
- refresh 成功后一起用新 access token 重试

这个保护只覆盖同一个页面实例。它不覆盖：

- 多标签页
- 多浏览器
- 多设备

### 11.3 后端 refresh 判断链

入口：

- `AuthController.refresh()`
- `AuthServiceImpl.refreshToken()`
- `LoginSessionManager.refreshSession()`

后端步骤：

1. 接收原始 refresh token
2. 计算 hash
3. 用 `refresh:{hash}` 查 `sessionId`
4. 用 `session:{sessionId}` 查 `LoginSession`
5. 判断 session 是否在线
6. 判断请求 refresh token hash 是否等于 session 当前 hash
7. 判断 `refreshExpireAt` 是否过期
8. 判断 `sessionExpireAt` 是否过期
9. 生成新 refresh token
10. 删除旧 refresh 索引
11. 更新 session：
    - `refreshTokenHash`
    - `accessTokenVersion + 1`
    - `refreshExpireAt`
    - `lastRefreshAt`
12. 写入 session 和新 refresh 索引
13. 签发新 access token
14. 返回新双 token

### 11.4 refresh 后旧 token 的命运

refresh 成功后：

- 旧 refresh token 失效，因为旧 `refresh:{oldHash}` 被删除
- 旧 access token 失效，因为 `accessTokenVersion` 已递增

所以 refresh 不是“补发一个新 token”，而是“推进这个 session 的有效 token 世代”。

---

## 12. 主动登出

前端调用：

```text
POST /api/auth/logout
```

后端流程：

1. 从 `Authorization` 里解析 access token
2. 取出 `sid`
3. 调用 `invalidateSession(sessionId)`
4. 删除 session 主记录
5. 删除 refresh 索引
6. 如果 `user:{userId}` 仍指向该 session，则删除 user 映射

前端流程：

1. 清 access token
2. 清 refresh token
3. 清 Vuex 会话状态
4. 重置动态路由状态
5. 回登录页

---

## 13. 管理员踢下线

前端入口：

- `web-admin/src/views/system/user/index.vue`

后端入口：

```text
DELETE /api/system/users/{id}/session
```

权限码：

```text
system:user:kickout
```

后端流程：

1. 校验目标用户存在
2. 防止当前管理员踢自己
3. 调用 `loginSessionManager.invalidateUserSession(id)`
4. 记录操作日志

被踢用户不会收到实时推送。它会在下一次请求时经历：

```text
业务请求 401
-> 前端尝试 refresh
-> refresh 失败
-> 清理本地 session
-> 跳回登录页
```

踢下线废掉的是会话，不是账号。只要账号仍启用，用户可以再次输入账号密码登录。

---

## 14. 删除用户、禁用用户、改密码的当前边界

这一节很重要，因为它关系到安全预期。

| 操作 | 当前是否立即清理已有 session | 说明 |
| --- | --- | --- |
| 管理员踢下线 | 是 | 调用 `invalidateUserSession(id)` |
| 删除用户 | 是 | `removeUser(id)` 中会先清理 session |
| 用户自己修改密码 | 否 | 只更新密码哈希 |
| 管理员重置密码 | 否 | 只更新密码哈希 |
| 管理员禁用用户 | 否 | 只修改数据库状态 |

所以按当前实现：

- 禁用用户会阻止未来登录，但不会让已登录 session 立刻失效
- 重置密码不会让该用户已登录端下线
- 自己修改密码后，当前登录仍继续有效

如果业务希望“禁用、改密、重置密码都强制全端下线”，需要在对应服务方法中显式调用 `invalidateUserSession(id)`，或者引入用户状态版本号，让 session 校验时能感知账号状态变化。

---

## 15. 401 排障矩阵

### 15.1 业务请求为什么 401

| 现象 | 可能原因 | 关键检查点 |
| --- | --- | --- |
| 请求没带 `Authorization` | 前端 token 丢失或拦截器未写入 | `request.js`、cookie |
| JWT 过期 | access token 到期 | `JwtTokenProvider.validateAccessToken()` |
| JWT 非 access 类型 | token 类型不对 | `typ` claim |
| token 中 `sid` 找不到 session | session 被删 | `session:{sid}` |
| session 不在线 | user 映射不指向当前 session | `user:{userId}` |
| access 版本落后 | 已 refresh 或被新 token 世代替换 | `av` vs `accessTokenVersion` |
| token userId 与 session userId 不一致 | token/session 不匹配 | `sub` vs `session.userId` |

### 15.2 refresh 为什么失败

| 现象 | 可能原因 | 关键检查点 |
| --- | --- | --- |
| refresh token 缺失 | 前端本地没有 refresh token | cookie、Vuex |
| `refresh:{hash}` 不存在 | refresh 已轮换、登出、踢下线、二次登录 | Redis refresh 索引 |
| session 主记录不存在 | 会话已被回收 | `session:{sessionId}` |
| session 不在线 | user 映射不再指向当前 session | `user:{userId}` |
| hash 不等于 session 当前 hash | 旧 refresh token 被重复使用 | `refreshTokenHash` |
| `refreshExpireAt` 到期 | 单次刷新窗口过期 | session 时间字段 |
| `sessionExpireAt` 到期 | 会话绝对上限到期 | session 时间字段 |

### 15.3 业务请求 401 但 refresh 能成功

这是正常场景。

常见原因：

- access token 自然过期
- access token 版本落后

只要 refresh token 和 session 仍有效，前端就能刷新成功并重试原请求。

### 15.4 业务请求 401 且 refresh 也失败

这通常意味着服务端已经不再承认这次登录。

常见原因：

- 用户主动登出
- 管理员踢下线
- 同账号第二次登录顶掉旧端
- refresh token 已轮换后又被旧端使用
- session 已过绝对上限
- 用户被删除导致 session 被回收

---

## 16. 几条典型时序

### 16.1 首次登录

```text
浏览器
-> POST /api/auth/login
-> gateway 白名单放行
-> AuthController.login()
-> AuthServiceImpl.login()
-> buildLoginUser()
-> LoginSessionManager.createSession()
-> invalidateUserSession(userId)
-> Redis 查不到旧 session
-> 生成 sessionId / refreshToken / refreshHash
-> persistSession()
-> 写入 user/session/refresh 三类 key
-> createAccessToken()
-> 返回双 token
-> 前端保存 token
-> GET /api/auth/user/profile
-> 生成动态路由
```

### 16.2 同账号第二次登录

```text
浏览器 B
-> POST /api/auth/login
-> LoginSessionManager.createSession()
-> invalidateUserSession(userId)
-> Redis 查到 oldSessionId
-> invalidateSession(oldSessionId)
-> 删除 old session
-> 删除 old refresh 索引
-> 删除 old user 映射
-> 生成 newSessionId / newRefreshToken / newHash
-> 写入新三类 key
-> 返回新双 token
```

浏览器 A 后续：

```text
浏览器 A 带旧 access token 请求
-> getValidSession(oldToken)
-> old sid 对应 session 不存在，或 user 映射不再指向它
-> 401
-> 尝试 old refresh token
-> old refresh 索引不存在
-> refresh 失败
-> 回登录页
```

### 16.3 access token 过期后的正常恢复

```text
业务请求
-> access token 过期
-> gateway 返回 401
-> 前端调用 /api/auth/refresh
-> refresh token hash 命中 session
-> session 在线，refresh 未过期，session 未到上限
-> 轮换 refresh token
-> accessTokenVersion + 1
-> 返回新双 token
-> 前端重试原请求
-> 成功
```

### 16.4 管理员踢下线

```text
管理员
-> DELETE /api/system/users/{id}/session
-> UserServiceImpl.kickout()
-> invalidateUserSession(id)
-> invalidateSession(sessionId)
-> 删除 session / refresh / user 映射
```

被踢用户：

```text
下一次请求
-> 401
-> refresh 失败
-> 清理本地状态
-> 跳回 /login
```

---

## 17. 并发与竞态

### 17.1 `invalidateSession()` 删除 user 映射前为什么要比对

`invalidateSession(sessionId)` 删除 `user:{userId}` 前，会先检查当前 user 映射是否仍等于正在失效的 sessionId。

这是为了避免误删新登录。

典型竞态：

1. 旧 session 正在失效
2. 用户同时完成新登录
3. `user:{userId}` 已经指向 `newSessionId`
4. 旧失效逻辑如果无条件删除 user key，就会把新登录也破坏

所以当前实现只在 user key 仍指向旧 session 时才删除。

### 17.2 前端只解决单实例 refresh 并发

`refreshPromise` 能合并同一个页面实例里的多次 401。

但它不能解决：

- 多标签页同时 refresh
- 多浏览器同时 refresh
- 多设备同时 refresh
- 后端同一 refresh token 的并发提交

### 17.3 后端 refresh 当前没有显式锁

按当前代码结构，`refreshSession()` 没有使用 Redis 分布式锁或 CAS。

因此多个请求几乎同时拿同一个旧 refresh token 刷新时，存在竞态窗口。可能结果包括：

- 一个成功，另一个失败
- 最后写入的 refresh token 成为唯一有效 token
- 某些时序下，后到请求发现 hash 不一致后触发 `invalidateSession(sessionId)`，导致已经刷新成功的 session 被整体作废

这不是日常单请求路径里的必现问题，但它是后续增强会话系统时最值得优先处理的点。

可选增强方向：

- refresh 时对 `sessionId` 加短 TTL 分布式锁
- 用 Redis Lua 脚本原子校验并替换 refresh 索引
- 引入 token family 与 reuse detection 策略，明确区分正常并发和疑似盗用

---

## 18. 安全语义总结

### 18.1 当前已经具备的能力

- access token 短期有效
- refresh token 轮换
- 旧 refresh token 立即作废
- refresh 后旧 access token 立即失效
- 单账号单活跃会话
- 主动登出回收服务端 session
- 管理员可强制踢下线
- 删除用户前清理 session
- session 有绝对续签上限

### 18.2 当前没有自动覆盖的能力

- 禁用用户后立即踢下线
- 改密码后立即踢下线
- 管理员重置密码后立即踢下线
- 多标签页 refresh 后端原子化
- 多设备 refresh 并发治理
- 多端在线设备列表
- refresh token 复用检测后的安全告警

这些不是文档推测，而是从当前代码行为得出的边界。

---

## 19. 如何顺代码

如果要自己阅读源码，建议按这个顺序：

1. `AuthServiceImpl.login()`
2. `LoginSessionManager.createSession()`
3. `LoginSessionManager.persistSession()`
4. `JwtTokenProvider.createAccessToken()`
5. `AuthTokenGlobalFilter.filter()`
6. `LoginSessionManager.getValidSession()`
7. `web-admin/src/utils/request.js`
8. `LoginSessionManager.refreshSession()`
9. `LoginSessionManager.invalidateSession()`
10. `LoginSessionManager.invalidateUserSession()`
11. `UserServiceImpl.kickout()`
12. `UserServiceImpl.removeUser()`

这个顺序能把主线串起来：

```text
登录 -> 鉴权 -> 401 -> refresh -> token 世代推进 -> 登出/踢下线/顶号
```

---

## 20. 常见误解

### 20.1 “这是 JWT 登录，所以服务端无状态”

不对。

当前 access token 是 JWT，但登录态是服务端 Redis session。没有 Redis 中有效 session，JWT 自己不能完成最终认证。

### 20.2 “access token 没过期就一定能用”

不对。

它还要满足：

- session 存在
- session 在线
- user 映射仍指向该 session
- access token 版本号匹配

### 20.3 “refresh token 在本地就一定能刷新”

不对。

它还要满足：

- refresh 索引存在
- session 存在
- session 在线
- hash 是 session 当前 hash
- refresh 窗口没过期
- session 绝对上限没过期

### 20.4 “踢下线等于禁用账号”

不对。

踢下线只废掉当前 session。账号如果仍启用，可以重新登录。

### 20.5 “禁用用户会让他立刻掉线”

按当前实现，不会。

禁用只影响后续登录，不会主动回收已经存在的 session。

### 20.6 “改密码会让所有端退出”

按当前实现，不会。

修改密码和重置密码都没有调用 session 失效逻辑。

---

## 21. 一句话总括

当前项目的认证体系可以这样理解：

```text
access token 负责让请求进入鉴权流程；
refresh token 负责申请推进 token 世代；
LoginSession 负责决定这次登录是否仍被服务端承认。
```

所有关键行为，包括单端在线、自动 refresh、refresh token rotation、旧 access token 立即失效、登出、踢下线、删除用户清 session，最终都围绕同一个 `sessionId` 和 Redis 中的三类 key 运转。
