# Backend Manager 认证与会话全流程详解 v2

本文档基于现有 `Logic.md` 和当前代码实现整理，目标不是罗列接口，而是把这套认证体系的运行机制、状态变化、失效条件、时序关系、排查抓手讲清楚。

适合解决这几类问题：

- 想理解这套项目到底是“JWT 登录”还是“Session 登录”
- 想搞清楚 `access token`、`refresh token`、`session` 三者谁负责什么
- 想知道登录、访问接口、刷新 token、登出、被踢下线、同账号再次登录时到底发生了什么
- 想分析 401 从哪里来，是 JWT 过期、session 失效，还是 refresh token 已经轮换
- 想顺着代码快速定位关键入口

本文默认以当前仓库实现为准，而不是以抽象理论为准。

---

## 1. 先看结论：真正决定登录态的是谁

这套系统表面上是“双 token”，但真正决定“当前登录是否仍然有效”的，不是浏览器里那两个 token，而是 Redis 中那条 `LoginSession` 记录。

可以先记住这个核心判断：

- `access token` 没过期，不代表服务端一定继续承认这次登录
- `refresh token` 还在客户端手里，不代表它还可以继续换发
- 只有 Redis 中当前 `session` 仍然有效，并且仍然被服务端承认，这次登录才算真的在线

更准确地说：

- `access token` 是访问业务接口的短期凭证
- `refresh token` 是换发新 token 的续签凭证
- `session` 是服务端保存的“这次登录记录”

如果更喜欢现实类比，可以这样理解：

- `access token`：门禁卡
- `refresh token`：续卡凭证
- `session`：物业系统里这次住户登记记录

真正说了算的是物业系统里的登记记录，不是你手里那张卡本身。

---

## 2. 当前实现属于什么认证模型

当前项目不是纯粹的“只靠 JWT 自证明身份”的方案，而是：

- 用 JWT 作为访问令牌
- 用随机字符串作为刷新令牌
- 用 Redis Session 作为服务端登录状态中心

也就是典型的：

`JWT access token + opaque refresh token + server-side session`

这意味着：

- token 只是访问与续签的凭证
- 在线状态、单端登录、强制下线、刷新轮换、旧 token 失效，都由服务端 session 统一控制

这种模型比单 token 方案更重，但换来了几个非常关键的能力：

- 单账号单活跃会话
- refresh token 轮换
- refresh 后旧 access token 立刻失效
- 管理员可强制踢人下线
- 会话续签有绝对上限，不会无限续命

---

## 3. 核心对象与职责

## 3.1 `access token`

代码位置：

- `backend/backend-common/src/main/java/com/example/common/security/JwtTokenProvider.java`

特点：

- 类型：JWT
- 默认有效期：`1800s`，即 30 分钟
- 主要用途：访问业务接口
- 前端传输方式：放到 `Authorization: Bearer <token>` 请求头

当前 token 中最关键的 claim：

| claim | 含义 |
| --- | --- |
| `sub` | 用户 ID |
| `username` | 用户名 |
| `nickname` | 昵称 |
| `sid` | 当前会话 ID |
| `typ` | token 类型，当前固定为 `access` |
| `av` | `accessTokenVersion`，用于版本控制 |

重要结论：

- access token 可被解析
- 但它不是最终裁决者
- 服务端仍然要根据其中的 `sid` 去 Redis 校验 `session`

## 3.2 `refresh token`

代码位置：

- `backend/backend-common/src/main/java/com/example/common/security/LoginSessionManager.java`

特点：

- 类型：高强度随机字符串，不是 JWT
- 默认单次续签窗口：`604800s`，即 7 天
- 主要用途：调用 `/api/auth/refresh` 换发新 token
- 前端存储方式：cookie
- 传输方式：只在 refresh 接口里作为请求体字段传给后端

示例：

```json
{
  "refreshToken": "xxxx"
}
```

为什么不是 JWT：

- refresh token 不需要被 gateway 或其他业务服务直接解析
- 它只服务于 auth 刷新流程
- 用随机串 + 服务端哈希存储，更适合做轮换和立即作废

## 3.3 `session`

代码位置：

- `backend/backend-common/src/main/java/com/example/common/model/security/LoginSession.java`

字段如下：

| 字段 | 含义 |
| --- | --- |
| `sessionId` | 本次登录的唯一会话编号 |
| `userId` | 登录用户 ID |
| `loginUser` | 登录用户快照 |
| `refreshTokenHash` | 当前有效 refresh token 的哈希 |
| `accessTokenVersion` | 当前 access token 版本号 |
| `refreshExpireAt` | 当前 refresh token 过期时间 |
| `sessionExpireAt` | 当前 session 绝对过期时间 |
| `lastRefreshAt` | 最近一次 refresh 时间 |

一句话总结：

`session` 才是“这次登录”的主体记录。

它回答的问题是：

- 这次登录是谁
- 当前会话是否仍然在线
- 当前 refresh token 是否仍然是最新那一个
- 当前 access token 的有效版本号是多少
- 会话还能续签到什么时候

## 3.4 `LoginUser`

代码位置：

- `backend/backend-common/src/main/java/com/example/common/model/security/LoginUser.java`
- 构建逻辑：`backend/backend-auth/src/main/java/com/example/auth/service/impl/AuthServiceImpl.java`

它是登录成功后保存在 session 内的用户快照，而不是数据库用户表实体本身。

通常包含：

- 用户 ID
- 用户名
- 昵称
- 部门信息
- 角色列表
- 权限列表

它的价值在于：

- 鉴权通过后，下游服务可以直接使用 session 里的快照
- 不必每次请求都重新拼装一遍完整权限模型

## 3.5 `accessTokenVersion`

这个字段非常关键，它解决的是：

“JWT 明明还没过期，为什么也要立刻失效”

机制如下：

1. 登录时，session 版本号初始化为 `1`
2. 签发 access token 时，把这个版本号写进 `av`
3. refresh 时，session 的版本号加 `1`
4. 新签发的 access token 也带上新的 `av`
5. 之后服务端校验请求时，必须比较：
   - JWT 里的 `av`
   - Redis session 里的 `accessTokenVersion`

只有一致才算有效。

因此：

- 旧 access token 即使还没到 JWT 的 `exp`
- 只要版本号落后
- 也会被判定为失效

---

## 4. Redis 中 3 类 key 的职责

代码位置：

- `backend/backend-common/src/main/java/com/example/common/constant/RedisKeyConstants.java`

当前登录体系使用 3 类 Redis key：

## 4.1 `admin:login:user:{userId}` -> `sessionId`

含义：

- 这个用户“当前被服务端承认的活跃会话”是谁

作用：

- 单账号单活跃会话
- 按用户踢下线
- 第二次登录时顶掉第一次登录

可以理解成：

```text
用户 -> 当前在线会话
```

## 4.2 `admin:login:session:{sessionId}` -> `LoginSession`

含义：

- 某个 `sessionId` 对应的完整登录记录

作用：

- 鉴权主记录
- 获取当前登录用户信息
- 校验 access token 版本
- 校验 refresh token 是否仍属于该 session
- 控制刷新窗口与绝对过期时间

可以理解成：

```text
会话编号 -> 这次登录的完整档案
```

## 4.3 `admin:login:refresh:{refreshTokenHash}` -> `sessionId`

含义：

- 某个 refresh token 哈希当前归属哪个 session

作用：

- refresh 接口根据 refresh token 找到 session
- 做 refresh token rotation
- 让旧 refresh token 立即作废

可以理解成：

```text
refresh token -> 属于哪次登录
```

## 4.4 为什么正好是这 3 类 key

因为系统要同时解决 3 个方向的问题：

- 按用户找当前在线会话
- 按会话找完整登录记录
- 按 refresh token 找所属会话

如果少任意一类：

- 少 `user -> session`：难以做单端在线和按用户踢人
- 少 `session -> LoginSession`：无法统一管理完整登录状态
- 少 `refreshHash -> session`：refresh 接口无法从 refresh token 反查会话

---

## 5. 一次成功登录后的 Redis 状态长什么样

假设用户 `1001` 成功登录，生成了：

- `sessionId = s_abc`
- `refreshTokenHash = h_xyz`

那么 Redis 中大致是：

```text
admin:login:user:1001             -> s_abc
admin:login:session:s_abc         -> LoginSession{...}
admin:login:refresh:h_xyz         -> s_abc
```

同时浏览器中保存：

- `accessToken`
- `refreshToken`

注意：

- 浏览器保存的是原始 refresh token
- Redis 保存的是 refresh token 的哈希

---

## 6. 全流程总览

下面先给一张总览图，再逐段展开。

```text
登录:
前端提交账号密码
-> auth 校验账号密码
-> 创建 LoginUser
-> createSession
-> 写入 3 类 Redis key
-> 返回 accessToken + refreshToken
-> 前端保存 token
-> 前端再调 profile 拉取菜单和权限

正常访问:
前端带 Authorization
-> gateway 校验 token 和 session
-> 下游 auth/system 再校验一次并恢复上下文
-> 业务执行

401 后续签:
前端业务请求收到 401
-> 尝试 /api/auth/refresh
-> auth 根据 refresh token 找到 session
-> 校验 session 与 refresh 是否仍有效
-> 轮换 refresh token
-> accessTokenVersion + 1
-> 返回新双 token
-> 前端重试原请求

主动退出或被踢:
invalidateSession / invalidateUserSession
-> 删除 session 主记录
-> 删除 refresh 索引
-> 必要时删除 user -> session
-> 之后旧 access token 和旧 refresh token 都不可用
```

---

## 7. 登录流程详解

## 7.1 前端提交登录

代码位置：

- `web-admin/src/views/login/index.vue`
- `web-admin/src/store/modules/user.js`
- `web-admin/src/api/auth.js`

前端提交链路：

1. 页面提交表单
2. 调用 `user/login`
3. 发送 `POST /api/auth/login`

## 7.2 auth 校验账号密码

代码位置：

- `backend/backend-auth/src/main/java/com/example/auth/controller/AuthController.java`
- `backend/backend-auth/src/main/java/com/example/auth/service/impl/AuthServiceImpl.java`

`AuthServiceImpl.login()` 主要做这些事：

1. 按用户名查询用户
2. 判断用户是否存在
3. 判断是否逻辑删除
4. 判断账号是否禁用
5. 校验密码
6. 构建 `LoginUser`
7. 调用 `loginSessionManager.createSession(loginUser)`
8. 记录登录日志

补充一点：

- 如果用户没有角色，`buildLoginUser()` 会直接抛异常
- 因为后续前端需要基于角色、菜单、权限构建路由

## 7.3 `createSession()` 真正做了什么

代码位置：

- `backend/backend-common/src/main/java/com/example/common/security/LoginSessionManager.java`

执行顺序非常关键：

1. 先调用 `invalidateUserSession(userId)`
2. 生成新的 `sessionId`
3. 生成新的 `refreshToken`
4. 对 refresh token 做 SHA-256 哈希
5. 组装新的 `LoginSession`
6. 设置 `accessTokenVersion = 1`
7. 计算两个过期时间：
   - `refreshExpireAt`
   - `sessionExpireAt`
8. 把 session 持久化到 Redis
9. 签发新的 access token
10. 返回新的双 token

这里最重要的一步是第 1 步：

`invalidateUserSession(userId)`

这就是当前“单账号单活跃会话”的实现入口。

也就是说：

- 同一个账号第二次登录时
- 系统会先废掉旧会话
- 再创建新会话

## 7.3.1 第一次登录的精确代码路径

这里说的“第一次登录”，指的是：

- 当前用户此前没有有效在线会话
- 或者 Redis 中已经没有该用户对应的 `user -> session` 映射

这时整条链路会经过这些代码：

1. 前端页面提交登录表单
   - `web-admin/src/views/login/index.vue`
2. Vuex 发起登录动作
   - `web-admin/src/store/modules/user.js`
3. 前端请求接口
   - `web-admin/src/api/auth.js`
   - `POST /api/auth/login`
4. 请求进入 gateway
   - `backend/backend-gateway/src/main/java/com/example/gateway/filter/AuthTokenGlobalFilter.java`
5. gateway 发现 `/api/auth/login` 在白名单中，直接放行
   - 白名单配置：`backend/backend-gateway/src/main/resources/application.yml`
6. auth 控制器接收请求
   - `AuthController.login()`
7. auth 服务处理登录
   - `AuthServiceImpl.login()`
8. `AuthServiceImpl.login()` 构建 `LoginUser`
   - `buildLoginUser(user)`
9. 调用 `LoginSessionManager.createSession(loginUser)`
10. `createSession()` 内部先执行 `invalidateUserSession(userId)`
11. `invalidateUserSession(userId)` 读取：
    - `admin:login:user:{userId}`
12. 因为是第一次登录，通常这里拿不到有效 `sessionId`
13. 于是走到“兜底清理”分支，仅删除可能存在的脏 user key
14. 回到 `createSession()`，开始真正创建新会话：
    - 生成 `sessionId`
    - 生成 `refreshToken`
    - 计算 `refreshTokenHash`
    - 组装 `LoginSession`
15. 调用 `persistSession(session, refreshTokenHash)` 写入 Redis：
    - `admin:login:user:{userId}` -> `sessionId`
    - `admin:login:session:{sessionId}` -> `LoginSession`
    - `admin:login:refresh:{refreshTokenHash}` -> `sessionId`
16. 调用 `toAuthTokens(session, refreshToken)` 签发 access token
17. `AuthServiceImpl.login()` 把 `AuthTokens` 转成 `LoginVO`
18. 前端收到响应后写入 cookie 和 Vuex：
    - `setAuthTokens(...)`
    - `SET_TOKENS`
19. 路由守卫检测到“已有 token 但未加载路由”，再调用：
    - `GET /api/auth/user/profile`
20. auth/profile 接口根据当前 access token 恢复登录用户上下文，返回资料、权限、菜单
21. 前端生成动态路由，第一次登录初始化完成

这条链路的关键特征是：

- `invalidateUserSession(userId)` 会被调用，但通常不会真正失效任何旧 session
- 整个过程更像是“空环境下创建第一条登录记录”
- Redis 最终从“无映射”进入“有完整 3 类 key”的状态

第一次登录前后的 Redis 变化通常是：

```text
登录前:
admin:login:user:{userId}         -> 不存在
admin:login:session:*             -> 不存在
admin:login:refresh:*             -> 不存在

登录后:
admin:login:user:{userId}         -> newSessionId
admin:login:session:{newSessionId} -> LoginSession
admin:login:refresh:{newHash}     -> newSessionId
```

## 7.3.2 第二次登录的精确代码路径

这里说的“第二次登录”，指的是：

- 同一个用户已经存在一个仍被服务端承认的在线 session
- 现在再次输入账号密码登录

它前半段代码路径和第一次登录完全一样，直到进入 `createSession()` 前都没有区别：

1. 前端提交登录表单
2. `user/login`
3. `POST /api/auth/login`
4. gateway 白名单放行
5. `AuthController.login()`
6. `AuthServiceImpl.login()`
7. 校验用户、密码、状态
8. `buildLoginUser(user)`
9. `LoginSessionManager.createSession(loginUser)`

真正的分叉从这里开始：

1. `createSession()` 先调用 `invalidateUserSession(userId)`
2. `invalidateUserSession(userId)` 读取：
   - `admin:login:user:{userId}`
3. 这一次能读到旧的 `oldSessionId`
4. 因为 `oldSessionId` 有值，所以调用：
   - `invalidateSession(oldSessionId)`
5. `invalidateSession(oldSessionId)` 内部执行：
   - 读取 `admin:login:session:{oldSessionId}`
   - 删除 `admin:login:session:{oldSessionId}`
   - 删除 `admin:login:refresh:{oldRefreshTokenHash}`
   - 读取 `admin:login:user:{userId}`
   - 如果它仍指向 `oldSessionId`，则删除这个 `user -> session`
6. 旧会话的整套登录状态被回收后，流程返回 `createSession()`
7. `createSession()` 再生成新的：
   - `newSessionId`
   - `newRefreshToken`
   - `newRefreshTokenHash`
   - `new LoginSession`
8. 再次调用 `persistSession(session, newRefreshTokenHash)` 写入新的 3 类 Redis key
9. 再调用 `toAuthTokens(...)` 签发新的 access token
10. 返回新的 `LoginVO`
11. 前端覆盖本地原有 token
12. 后续 profile 拉取、动态路由恢复与第一次登录一致

第二次登录和第一次登录相比，最关键的区别不是“后面多了什么”，而是：

- 第二次登录在创建新 session 之前，会先完整回收旧 session
- 这一步决定了旧设备、旧浏览器、旧页面里的登录态会被挤下线

第二次登录前后的 Redis 变化通常是：

```text
第二次登录前:
admin:login:user:{userId}              -> oldSessionId
admin:login:session:{oldSessionId}     -> old LoginSession
admin:login:refresh:{oldHash}          -> oldSessionId

执行 invalidateSession(oldSessionId) 后:
admin:login:user:{userId}              -> 删除
admin:login:session:{oldSessionId}     -> 删除
admin:login:refresh:{oldHash}          -> 删除

createSession() 创建新会话后:
admin:login:user:{userId}              -> newSessionId
admin:login:session:{newSessionId}     -> new LoginSession
admin:login:refresh:{newHash}          -> newSessionId
```

从代码语义上看，第二次登录做的是两段式处理：

1. 回收旧登录
2. 创建新登录

所以第二次登录不是在“复用旧 session”，而是在“先清旧，再建新”。

## 7.3.3 第一次登录与第二次登录的最小差异点

如果只抓最核心差异，可以只看 `LoginSessionManager.createSession()` 开头这一句：

```java
invalidateUserSession(loginUser.getUserId());
```

第一次登录时，这句调用的结果通常是：

- 查不到有效旧 session
- 只做一次兜底清理
- 不会真正进入旧 session 回收链

第二次登录时，这句调用的结果通常是：

- 查到旧 `sessionId`
- 进入 `invalidateSession(oldSessionId)`
- 把旧 session、旧 refresh 索引、旧 user 映射全部回收

也就是说，第一次登录和第二次登录的大多数代码路径是相同的，真正不同的是：

- `invalidateUserSession(userId)` 这一跳之后是否能查到旧 `sessionId`
- 一旦查到，就会多经过一整段旧会话失效链路

这也是分析“为什么同账号第二次登录会把第一次挤掉”时最值得盯住的代码分叉点。

## 7.4 前端收到登录结果后做什么

代码位置：

- `web-admin/src/store/modules/user.js`
- `web-admin/src/utils/auth.js`

前端处理：

1. 把 `accessToken` 写入 cookie
2. 把 `refreshToken` 写入 cookie
3. 同步写入 Vuex

但这时还没完成“完整进入系统”。

原因是：

- 现在只是拿到了双 token
- 前端还没拿到当前用户资料、角色、权限、菜单树

## 7.5 为什么登录后还要再调一次 profile

代码位置：

- `web-admin/src/main.js`
- `web-admin/src/api/auth.js`
- `backend/backend-auth/src/main/java/com/example/auth/controller/ProfileController.java`

路由守卫会在“有 token 但动态路由还没加载”时执行：

1. 调用 `GET /api/auth/user/profile`
2. 拉取当前用户资料、角色、权限、菜单
3. 动态生成路由
4. 把用户导向可访问首页

所以登录后的前端初始化实际上分两步：

1. `/api/auth/login`
   - 获取双 token
2. `/api/auth/user/profile`
   - 获取资料、权限、菜单并生成路由

---

## 8. 正常访问业务接口时发生了什么

## 8.1 前端如何带 token

代码位置：

- `web-admin/src/utils/request.js`

请求拦截器会：

1. 从 cookie 取出最新 `accessToken`
2. 写入请求头：

```http
Authorization: Bearer <accessToken>
```

注意：

- access token 是主动写入请求头
- 不是依赖浏览器自动附带 cookie 认证

## 8.2 gateway 如何校验

代码位置：

- `backend/backend-gateway/src/main/java/com/example/gateway/filter/AuthTokenGlobalFilter.java`

gateway 对非白名单请求会做这些检查：

1. 解析 `Authorization`
2. 提取 access token
3. 调用 `loginSessionManager.getValidSession(token)`
4. 如果返回 `null`，直接返回 `401`
5. 如果 session 有效，则把基础用户信息透传给下游

而 `getValidSession(token)` 内部又做了更细的判断：

1. JWT 是否合法
2. JWT 是否过期
3. 根据 `sid` 读取 `session -> LoginSession`
4. `session` 是否仍在线
5. JWT 中的 `av` 是否等于 session 中的 `accessTokenVersion`
6. JWT 中的 `userId` 是否与 session 中一致

只有全部通过，才算当前请求已登录。

## 8.3 `isSessionOnline()` 实际在检查什么

`isSessionOnline(session, sessionId)` 做的不是抽象意义上的“在线”，而是：

1. session 主记录存在
2. `sessionId` 不为空
3. `loginUser` 存在
4. `admin:login:user:{userId}` 当前仍指向这个 `sessionId`

也就是说，只有满足：

`user -> session` 仍然指向当前 session

这个 session 才被视为“当前被承认的在线会话”。

这一步正是“单账号只能有一个当前活跃会话”的关键判定。

## 8.4 为什么下游服务还要再验一层

代码位置：

- `backend/backend-auth/src/main/java/com/example/auth/security/JwtAuthenticationFilter.java`
- `backend/backend-system/src/main/java/com/example/system/security/JwtAuthenticationFilter.java`

即使 gateway 已经校验过，下游 auth/system 仍会再次调用：

- `loginSessionManager.getValidSession(token)`

目的有两个：

- 防止绕过 gateway 直连下游服务时失去保护
- 恢复 `SecurityContextHolder` 和 `LoginUserContext`

换句话说：

- gateway 是第一层登录态校验
- auth/system 是第二层校验和上下文恢复

## 8.5 业务层怎么拿当前登录人

代码位置：

- `backend/backend-common/src/main/java/com/example/common/context/LoginUserContext.java`

当下游过滤器验证通过后，会把 `LoginUser` 放进：

- `SecurityContextHolder`
- `LoginUserContext`

这样业务层就可以通过 `LoginUserContext.get()` 或权限注解正常拿到当前登录用户。

---

## 9. Refresh 流程详解

这是最容易混淆的一段，也是双 token 方案最核心的一段。

## 9.1 先记住一个结论

`access token` 没过期，也完全可以 refresh 出新的 `access token`。

因为 refresh 看的是：

- refresh token 是否有效
- session 是否有效

而不是看旧 access token 是否已经到 `exp`。

## 9.2 前端什么时候触发 refresh

代码位置：

- `web-admin/src/utils/request.js`

当前前端策略是：

当业务请求返回 `401` 时，如果满足这些条件，就优先尝试 refresh：

1. 当前请求不是 `/api/auth/refresh`
2. 当前请求还没有重试过
3. 当前本地还有 refresh token

然后执行：

1. `POST /api/auth/refresh`
2. 成功则覆盖本地双 token
3. 用新的 access token 重试原请求

如果 refresh 失败，则：

1. 清理本地登录态
2. 跳转登录页

## 9.3 服务端 refresh 做了什么

入口：

- `AuthController.refresh()`
- `AuthServiceImpl.refreshToken()`
- `LoginSessionManager.refreshSession()`

执行顺序：

1. 接收原始 refresh token
2. 做 SHA-256 哈希
3. 用 `admin:login:refresh:{hash}` 找到 `sessionId`
4. 用 `sessionId` 读取 `admin:login:session:{sessionId}`
5. 校验 session 是否仍在线
6. 校验当前 refresh 哈希是否等于 session 中记录的那一个
7. 校验：
   - `refreshExpireAt` 未过期
   - `sessionExpireAt` 未过期
8. 生成新的 refresh token
9. 生成新的 refresh 哈希
10. 删除旧 refresh 索引
11. 更新 session：
    - `refreshTokenHash`
    - `accessTokenVersion = accessTokenVersion + 1`
    - `refreshExpireAt`
    - `lastRefreshAt`
12. 重新写回 Redis
13. 签发新的 access token
14. 返回新的双 token

## 9.4 为什么 refresh 后旧 access token 会失效

因为 refresh 时，session 的 `accessTokenVersion` 会加 1。

示例：

1. 登录后：
   - session.version = 1
   - accessToken.av = 1
2. refresh 后：
   - session.version = 2
   - newAccessToken.av = 2
3. 旧 access token 再来访问时：
   - token.av = 1
   - session.version = 2
   - 版本不一致，判定失效

所以：

- 旧 access token 不是等自己自然过期
- 而是在 refresh 后就被逻辑失效了

## 9.5 为什么 refresh token 也会失效

因为当前实现启用了 refresh token rotation。

含义是：

- 每次 refresh 都会换发一个新的 refresh token
- 旧 refresh token 的索引会立刻删除

因此旧 refresh token 无法重复使用。

---

## 10. 会话过期策略

当前代码里的时间控制分为 3 层：

## 10.1 access token 过期时间

- 配置项：`security.jwt.access-expire-seconds`
- 当前默认：`1800s`
- 含义：单个 access token 的 JWT 有效期

## 10.2 refresh token 单次刷新窗口

- 配置项：`security.jwt.refresh-expire-seconds`
- 当前默认：`604800s`
- 含义：当前 refresh token 这一次还能用于 refresh 的时间

## 10.3 session 绝对过期上限

- 配置项：`security.jwt.refresh-max-expire-seconds`
- 当前默认：`2592000s`
- 含义：整个 session 最长可存活时间

当前算法是：

- `sessionExpireAt = now + refreshMaxExpireSeconds`
- `refreshExpireAt = min(now + refreshExpireSeconds, sessionExpireAt)`

这意味着：

- refresh token 不是无限续命
- 就算用户一直活跃刷新
- 整个 session 到绝对上限后也必须重新登录

---

## 11. 登出与失效流程

## 11.1 主动登出

入口：

- 前端：`POST /api/auth/logout`
- 后端：`AuthController.logout()` -> `AuthServiceImpl.logout()`

处理逻辑：

1. 从 `Authorization` 里解析 access token
2. 从 token 中取出 `sid`
3. 调用 `invalidateSession(sessionId)`
4. 删除相关 Redis 状态
5. 前端无论接口结果如何，最终都会清理本地 token

## 11.2 `invalidateSession(sessionId)` 到底做了什么

代码位置：

- `backend/backend-common/src/main/java/com/example/common/security/LoginSessionManager.java`

这不是简单“删一个 session key”，而是按一个已知 `sessionId` 去清理整套登录状态。

执行顺序：

1. 如果 `sessionId` 为空，直接返回
2. 读取 `admin:login:session:{sessionId}`
3. 删除 `admin:login:session:{sessionId}`
4. 如果 session 根本不存在，直接返回
5. 删除 `admin:login:refresh:{refreshTokenHash}`
6. 读取当前 `admin:login:user:{userId}`
7. 只有当它仍然等于当前 `sessionId` 时，才删除 `admin:login:user:{userId}`

这里最后一步非常重要。

它不是无脑删 `user -> session`，而是“确认当前映射还指向旧会话时才删”。

这样做是为了避免这种情况：

1. 旧会话正在被失效处理
2. 同时用户已经重新登录，`user -> session` 已经指向新 session
3. 如果这时无条件删除 `user -> session`
4. 就会误把新登录映射也删掉

因此这段逻辑本质上是在避免误删新会话。

## 11.3 `invalidateUserSession(userId)` 到底做了什么

它表示：

- 按用户找到当前活跃会话
- 再把这个会话整套作废

执行逻辑：

1. 根据 `admin:login:user:{userId}` 查 `sessionId`
2. 如果查到有效 `sessionId`，就调用 `invalidateSession(sessionId)`
3. 如果查不到有效 `sessionId`，直接删除这个 user key，清理脏数据

这里要特别注意一个语义点：

方法名看起来像“让用户所有会话失效”，但以当前 Redis 结构来看，它实际只会失效“当前唯一被承认的活跃会话”。

因为当前模型只有：

`userId -> 一个 sessionId`

所以当前实现本质上是单会话模型，而不是多设备并存模型。

---

## 12. 同账号再次登录时会发生什么

这部分就是“单账号单活跃会话”的真正落地过程。

执行顺序：

1. 用户再次输入账号密码登录
2. `createSession()` 先调用 `invalidateUserSession(userId)`
3. 系统找到旧 `sessionId`
4. 删除旧 `session -> LoginSession`
5. 删除旧 `refreshHash -> session`
6. 删除旧 `user -> session`
7. 创建新 `sessionId`
8. 写入新的 3 类 Redis key
9. 签发新的双 token

最终结果：

- 第一次登录的 access token 失效
- 第一次登录的 refresh token 也失效
- 第二次登录成为新的唯一有效登录

## 12.1 用“经过哪些代码”来对比第一次登录和第二次登录

为了更方便你顺着源码读，这里把两条路径并排列一下。

第一次登录经过的主链：

1. `index.vue` 提交表单
2. `store/modules/user.js` -> `login`
3. `api/auth.js` -> `POST /api/auth/login`
4. gateway 白名单放行
5. `AuthController.login()`
6. `AuthServiceImpl.login()`
7. `AuthServiceImpl.buildLoginUser()`
8. `LoginSessionManager.createSession()`
9. `LoginSessionManager.invalidateUserSession()`
10. 未命中旧 session，仅做兜底清理
11. `LoginSessionManager.persistSession()`
12. `LoginSessionManager.toAuthTokens()`
13. 前端保存 token
14. `GET /api/auth/user/profile`
15. 下游 `JwtAuthenticationFilter`
16. `LoginSessionManager.getValidSession()`
17. `LoginUserContext.set(...)`

第二次登录经过的主链：

1. `index.vue` 提交表单
2. `store/modules/user.js` -> `login`
3. `api/auth.js` -> `POST /api/auth/login`
4. gateway 白名单放行
5. `AuthController.login()`
6. `AuthServiceImpl.login()`
7. `AuthServiceImpl.buildLoginUser()`
8. `LoginSessionManager.createSession()`
9. `LoginSessionManager.invalidateUserSession()`
10. 命中旧 `sessionId`
11. `LoginSessionManager.invalidateSession(oldSessionId)`
12. 删除旧 session 主记录
13. 删除旧 refresh 索引
14. 删除旧 user -> session 映射
15. 返回 `createSession()`
16. `LoginSessionManager.persistSession()`
17. `LoginSessionManager.toAuthTokens()`
18. 前端覆盖旧 token
19. `GET /api/auth/user/profile`
20. 下游 `JwtAuthenticationFilter`
21. `LoginSessionManager.getValidSession()`
22. `LoginUserContext.set(...)`

从这个并排视角看，第二次登录比第一次登录多出来的关键代码就是：

- `invalidateUserSession()` 命中旧会话
- `invalidateSession(oldSessionId)` 这整段回收链

## 12.2 为什么旧端会在“下一次请求”才表现出失效

第二次登录成功的瞬间，旧端本地其实还保存着：

- 旧 access token
- 旧 refresh token

但服务端的 Redis 已经变成新状态了。

所以旧端不是立即收到推送，而是等到下一次请求时才暴露问题：

1. 旧端拿旧 access token 发业务请求
2. gateway 调用 `getValidSession(oldAccessToken)`
3. 从旧 token 里的 `sid` 找旧 session
4. 发现旧 session 已被删，或 `user -> session` 已不再指向它
5. 返回 `401`
6. 前端再尝试用旧 refresh token 调 `/api/auth/refresh`
7. auth 发现旧 `refreshHash -> session` 已经被删
8. refresh 失败
9. 前端清理本地状态并跳回登录页

也就是说，第二次登录把第一次登录“挤掉”的效果，并不是通过前端广播实现的，而是通过服务端状态切换实现的：

- 新端写入新的 3 类 Redis key
- 旧端的所有关键索引同时失效
- 旧端在下一次访问时自然被判定为未登录

## 12.3 一张对比表看懂第一次登录与第二次登录

| 对比项 | 第一次登录 | 第二次登录 |
| --- | --- | --- |
| 登录前 `user -> session` | 通常不存在 | 已存在并指向旧 session |
| 是否调用 `invalidateUserSession()` | 会调用 | 会调用 |
| `invalidateUserSession()` 是否命中旧会话 | 通常不会 | 通常会 |
| 是否进入 `invalidateSession(oldSessionId)` | 通常不会 | 会 |
| 是否删除旧 `session -> LoginSession` | 不涉及 | 会 |
| 是否删除旧 `refreshHash -> session` | 不涉及 | 会 |
| 是否删除旧 `user -> session` | 只可能清理脏数据 | 会删除旧映射 |
| 是否生成新的 `sessionId` | 会 | 会 |
| 是否写入新的 3 类 Redis key | 会 | 会 |
| 旧端是否会被挤下线 | 不存在旧端 | 会，下一次请求时体现 |

这张表的本质结论是：

- 两次登录的“创建新会话”部分几乎相同
- 差异主要集中在创建前是否需要先回收旧会话

旧端后续表现：

- 再访问业务接口会返回 `401`
- 再尝试 refresh 也会失败
- 前端清理本地状态并跳回登录页

所以更准确的说法是：

- 不是“第二次登录会失效”
- 而是“第二次登录会顶掉第一次登录，自己成为新的有效登录”

## 12.4 用时序图文字版看第一次登录、第二次登录、旧端失效

这一节不引入新概念，只把前面已经讲过的链路改写成更适合“脑内演算”的时序形式。

### 12.4.1 时序一：浏览器 A 第一次登录

```text
浏览器 A
-> POST /api/auth/login
-> gateway 白名单放行
-> AuthController.login()
-> AuthServiceImpl.login()
-> buildLoginUser()
-> LoginSessionManager.createSession()
-> invalidateUserSession(userId)
-> Redis 中查 user -> session，不存在
-> 返回 createSession()
-> 生成 newSessionId / newRefreshToken / newHash
-> persistSession()
-> Redis 写入 3 类 key
-> toAuthTokens()
-> 返回新双 token
-> 前端写 cookie / Vuex
-> GET /api/auth/user/profile
-> JwtAuthenticationFilter
-> getValidSession(accessToken)
-> 恢复 LoginUserContext
-> 返回 profile / menus / permissions
```

这个时序的核心特点是：

- 整条链路里没有“旧会话回收”
- 只有“检查旧映射是否存在”和“创建新会话”

### 12.4.2 时序二：浏览器 B 第二次登录，顶掉浏览器 A

```text
浏览器 B
-> POST /api/auth/login
-> gateway 白名单放行
-> AuthController.login()
-> AuthServiceImpl.login()
-> buildLoginUser()
-> LoginSessionManager.createSession()
-> invalidateUserSession(userId)
-> Redis 中查 user -> session，命中 oldSessionId
-> invalidateSession(oldSessionId)
-> 删除 old session 主记录
-> 删除 old refresh 索引
-> 删除 old user -> session 映射
-> 返回 createSession()
-> 生成 newSessionId / newRefreshToken / newHash
-> persistSession()
-> Redis 写入新的 3 类 key
-> toAuthTokens()
-> 返回新的双 token
-> 浏览器 B 覆盖本地 token
-> GET /api/auth/user/profile
-> 正常进入系统
```

这个时序的关键点是：

- 浏览器 B 登录成功的前提，不是“沿用 A 的 session”
- 而是“先销毁 A 的 session，再创建 B 自己的新 session”

### 12.4.3 时序三：浏览器 A 被顶掉后，下一次请求才感知失效

```text
浏览器 A
-> 带旧 access token 调业务接口
-> gateway 读取 Authorization
-> getValidSession(oldAccessToken)
-> 从旧 token 的 sid 找旧 session
-> 发现旧 session 已不存在，或 user -> session 已不再指向它
-> gateway 返回 401
-> 前端收到 401，尝试 /api/auth/refresh
-> refreshSession(oldRefreshToken)
-> 用 oldRefreshHash 查 Redis
-> 发现 old refresh 索引已被删除
-> refresh 失败
-> clearSessionAndRedirect()
-> 清本地 token
-> 跳回 /login
```

因此“顶号”的真实语义是：

- 新端登录时，服务端切换了承认的在线状态
- 旧端不是被前端主动通知下线
- 而是在下一次访问时，被服务端按新状态判定为失效

---

## 13. 管理员强制踢人下线

前端入口：

- `web-admin/src/views/system/user/index.vue`

后端入口：

- `DELETE /api/system/users/{id}/session`
- `UserController.kickout()`
- `UserServiceImpl.kickout()`

执行顺序：

1. 校验目标用户存在
2. 防止当前管理员把自己踢下线
3. 调用 `loginSessionManager.invalidateUserSession(id)`
4. 记录操作日志

被踢用户不会立即收到推送通知。

他是在“下一次请求”时感知失效：

1. 业务请求收到 `401`
2. 前端尝试 refresh
3. refresh 也失败
4. 前端提示登录态失效
5. 清理本地状态并跳回登录页

需要特别说明：

- 被踢下线处理的是会话层
- 不是账号层

所以如果账号仍处于可用状态，用户之后仍然可以重新输入账号密码再次登录。

---

## 14. 401、失效与状态矩阵

这部分最适合排查问题。

## 14.1 哪个状态缺了，会导致什么结果

| 丢失或异常状态 | 访问业务接口 | refresh | 典型原因 |
| --- | --- | --- | --- |
| access token 缺失 | 401 | 取决于 refresh 是否还在 | 前端未带 token 或 cookie 被清 |
| access token JWT 过期 | 401 | 可能成功 | 正常过期 |
| `session -> LoginSession` 不存在 | 401 | 通常失败 | 登出、被踢、二次登录顶掉、session TTL 到期 |
| `user -> session` 不再指向当前 session | 401 | 失败 | 二次登录顶掉旧会话 |
| `refreshHash -> session` 不存在 | 业务接口可能正常 | refresh 失败 | refresh 已轮换、会话已失效 |
| `token.av` 小于 session.version | 401 | 可能成功 | 已 refresh，但客户端还在用旧 access token |
| `refreshExpireAt` 已过期 | 业务接口可能暂时正常 | refresh 失败 | 长时间未刷新 |
| `sessionExpireAt` 已过期 | 401 或 refresh 失败 | 失败 | 会话达到绝对上限 |

## 14.2 为什么有时业务请求 401，但 refresh 还能成功

因为两者看的条件不完全一样。

业务请求主要看：

- access token 是否有效
- session 是否有效
- access token 版本是否匹配

refresh 主要看：

- refresh token 是否有效
- session 是否有效
- refresh 是否仍在刷新窗口内

因此常见情况是：

- 旧 access token 已过期或版本落后
- 但 refresh token 仍有效
- 于是前端先收到 401，再 refresh 成功，然后重试原请求

## 14.3 为什么有时 refresh 失败后就只能重新登录

因为 refresh 失败通常意味着：

- refresh token 已轮换
- refresh token 已过期
- session 已失效
- session 已达到绝对上限

这些都属于服务端已经不再承认当前登录态的情况。

## 14.4 细化异常场景矩阵

下面这张表比前面的状态矩阵更偏“排障视角”，强调每种异常在代码里会卡在哪一步。

| 场景 | 业务请求时会卡在哪 | refresh 时会卡在哪 | 前端最终表现 | 说明 |
| --- | --- | --- | --- | --- |
| access token 过期 | `validateAccessToken(token)` 返回 `false` | 可能成功 | 先 401，再自动 refresh | 最典型的正常续签场景 |
| access token 版本落后 | `getAccessTokenVersion(token)` 与 session.version 不一致 | 可能成功 | 先 401，再自动 refresh | 旧 access token 被新 token 顶掉的常见场景 |
| `session -> LoginSession` 被删 | `redisOperator.get(loginSessionKey)` 拿不到 session | 通常失败 | 401 后回登录页 | 登出、踢下线、被顶号、绝对过期都可能导致 |
| `user -> session` 不再指向当前 session | `isSessionOnline()` 返回 `false` | 通常失败 | 401 后回登录页 | 单账号第二次登录最典型 |
| refresh 索引被删 | 业务请求可能仍成功 | `loginRefreshKey(hash)` 查不到 sessionId | refresh 失败，回登录页 | refresh token 已轮换，或 session 已失效 |
| refresh token 过期，但 access token 还没过期 | 当前业务请求可能还能成功 | `refreshExpireAt <= now` | 一段时间后首个 401 无法恢复 | 常见于用户长期不操作后才再次访问 |
| session 绝对过期，但 access token 还没过期 | 业务请求可能先因 session 不在线失败 | `sessionExpireAt <= now` | 401 后 refresh 也失败 | 表面看 token 没过期，实则 session 已被服务端淘汰 |
| 用户被管理员禁用 | 当前已登录请求通常仍能通过 | refresh 通常也仍能成功 | 不会立即掉线 | 按当前实现，禁用影响未来登录，不主动回收现有 session |
| 用户被删除 | 旧请求很快 401 | refresh 失败 | 回登录页 | `removeUser(id)` 会先 `invalidateUserSession(id)` |
| 管理员重置了用户密码 | 当前已登录请求通常仍能通过 | refresh 通常也仍能成功 | 不会立即掉线 | 按当前实现，只改数据库密码，不回收现有 session |
| 当前用户主动修改密码 | 当前会话通常继续有效 | refresh 通常也仍能成功 | 不会立即掉线 | 只更新数据库密码，不回收当前 session |

## 14.5 并发与竞态说明

这部分很重要，因为认证流程看起来是线性的，但实际运行时经常有并发请求。

### 14.5.1 为什么 `invalidateSession()` 删除 `user -> session` 前要先比对当前值

当前代码不是直接删除：

- `admin:login:user:{userId}`

而是先读取当前值，再判断：

- 只有当它仍然等于“正在失效的那个 `sessionId`”时才删除

这么做是为了避免竞态误删。

一个典型场景：

1. 旧 session 正在执行失效逻辑
2. 同时用户已经成功完成了新一轮登录
3. `user -> session` 已经被新登录改写成 `newSessionId`
4. 如果旧逻辑还无条件删除 `user -> session`
5. 就会把新登录映射也删掉

因此这里的“先比对再删”，本质上是一个防误删保护。

### 14.5.2 前端如何避免同一页面内重复 refresh

代码位置：

- `web-admin/src/utils/request.js`

当前前端用了一个共享变量：

- `refreshPromise`

作用是：

- 如果同一个 SPA 实例里同时有多个请求收到 `401`
- 第一个请求发起 refresh 后，后续请求不会再重复发新的 refresh
- 而是直接等待同一个 `refreshPromise`

这可以减少：

- 同一页面内的重复 refresh
- 同一次 access token 失效导致的 refresh 风暴

但要注意，它只能约束：

- 同一个浏览器标签页里的同一个前端实例

它不能约束：

- 多个标签页
- 多个浏览器
- 多台设备

### 14.5.3 refresh 在多标签页/多设备并发下的当前风险

按当前后端代码推断，refresh 流程没有显式加锁，也没有 CAS 类原子保护。

这意味着如果两个请求几乎同时拿着同一个旧 refresh token 去刷新，可能出现竞态。

可能的结果包括：

1. 其中一个先成功刷新，另一个随后失败
2. 两个请求都进入刷新逻辑，但最后只有最后一次写入的 refresh token 仍然有效
3. 更极端地，如果一个请求已经把 session 中的 `refreshTokenHash` 更新成新值，另一个请求还拿着旧 hash 去校验，那么它会命中：
   - `!refreshTokenHash.equals(session.getRefreshTokenHash())`
   - 然后调用 `invalidateSession(sessionId)`

第 3 种情况意味着：

- 并发 refresh 在某些时序下，可能把本来已经成功刷新的 session 再次整体失效

这里我特意写成“按当前代码推断”，因为这属于竞态时序分析，不是单线程下必现的固定结果，但它确实是当前实现值得警惕的风险点。

### 14.5.4 文档层面的结论

如果从设计稳健性角度看，当前实现已经做了两层保护：

- 前端单实例 `refreshPromise`
- 后端 `invalidateSession()` 的 user 映射比对保护

但仍然没有完全消除：

- 多标签页 refresh 并发
- 多设备 refresh 并发
- 后端 refresh 竞态更新

所以如果后续要继续强化这套登录体系，这一块会是优先考虑的增强点。

---

## 15. 从源码视角看几个最关键的方法

## 15.1 `createSession(LoginUser loginUser)`

你可以把它看成：

“创建新登录并顶掉旧登录”

它负责：

- 单端在线入口
- session 初始化
- refresh token 初始化
- access token 首次签发

## 15.2 `getValidSession(String accessToken)`

你可以把它看成：

“对当前 access token 做最终登录态裁决”

它不是只验 JWT，而是同时校验：

- JWT 合法性
- session 是否存在
- session 是否仍在线
- 版本号是否匹配
- 用户 ID 是否一致

## 15.3 `refreshSession(String refreshToken)`

你可以把它看成：

“基于当前 session 做一次续签，并让旧 token 退役”

它负责：

- refresh token 反查 session
- refresh token rotation
- access token 版本推进
- 新双 token 签发

## 15.4 `invalidateSession(String sessionId)`

你可以把它看成：

“按会话 ID 做整套会话回收”

它回收的不是单个 key，而是一整组登录状态：

- session 主记录
- refresh 索引
- 必要时的 user -> session 映射

## 15.5 `invalidateUserSession(Long userId)`

你可以把它看成：

“按用户找到当前活跃会话并回收”

它是以下行为的共用底层能力：

- 同账号再次登录
- 管理员踢人下线
- 删除用户前清理在线状态

---

## 16. 前后端协作关系

## 16.1 前端负责什么

前端主要负责：

- 登录后保存双 token
- 业务请求带 access token
- 收到 401 后尝试 refresh
- refresh 成功后重试原请求
- refresh 失败后清理本地会话并回登录页
- 初次进入系统时拉取 profile 与动态路由

## 16.2 gateway 负责什么

gateway 主要负责：

- 作为统一入口
- 白名单放行
- 对业务请求做第一层登录态校验
- 提前拦截无效登录，直接返回 401

## 16.3 auth/system 负责什么

下游服务主要负责：

- 再次校验当前 token 与 session
- 恢复 `SecurityContextHolder`
- 恢复 `LoginUserContext`
- 供业务层与权限注解继续使用当前登录人

## 16.4 用户操作对现有 session 的实际影响

这一节很适合做安全评审，因为很多“用户管理动作”并不会自动影响当前在线 session。

| 操作 | 入口代码 | 是否立即影响当前在线 session | 当前行为说明 |
| --- | --- | --- | --- |
| 当前用户修改自己的密码 | `AuthServiceImpl.updatePassword()` | 否 | 只更新数据库密码，不调用 `invalidateSession()` 或 `invalidateUserSession()` |
| 管理员重置某用户密码 | `UserServiceImpl.resetPassword()` | 否 | 只更新数据库密码，不回收该用户现有 session |
| 管理员禁用某用户 | `UserServiceImpl.changeStatus()` | 否 | 只改数据库中的 `status`，已登录 session 仍按旧快照继续运行 |
| 管理员踢用户下线 | `UserServiceImpl.kickout()` | 是 | 调用 `invalidateUserSession(id)`，当前活跃 session 被回收 |
| 管理员删除用户 | `UserServiceImpl.removeUser()` | 是 | 先 `invalidateUserSession(id)`，再标记删除 |

这个表里最容易被误判的，是前 3 项：

- 改密码不会自动让当前登录态失效
- 重置密码不会自动让该用户所有已登录端下线
- 禁用用户不会自动让该用户立刻掉线

原因不是业务上一定应该这样，而是“当前代码确实就是这样实现的”。

### 16.4.1 为什么禁用用户后，已有会话通常还不会立刻失效

登录时，`AuthServiceImpl.login()` 会校验数据库里的：

- `user.getStatus()`

只有启用状态才允许创建 session。

但登录成功后，后续请求走的是：

- `getValidSession(accessToken)`

它检查的是：

- JWT 是否合法
- session 是否存在
- session 是否仍在线
- 版本号是否匹配

它并不会在每次请求时重新回数据库检查用户当前状态。

所以按当前实现：

- “禁用用户”主要影响未来登录
- 不会自动回收已经存在的 session

如果希望“禁用即掉线”，就需要在禁用动作里显式调用：

- `invalidateUserSession(id)`

或者在每次请求时增加额外的数据库状态校验。

### 16.4.2 为什么改密码或重置密码后，已有登录通常仍然有效

无论是：

- 当前用户 `updatePassword()`
- 管理员 `resetPassword()`

当前实现都只是更新数据库里的密码哈希。

它们都没有：

- 修改 `session.accessTokenVersion`
- 删除 `refreshHash -> session`
- 删除 `session -> LoginSession`
- 删除 `user -> session`

因此：

- 已经签发出去的 access token 仍按原逻辑校验
- 已经存在的 refresh token 仍按原 session 逻辑校验
- 当前在线会话不会自动退出

这也是文档里必须单独写出来的一个现状，因为很多人会默认“改密码等于全端下线”，但当前代码并不是这样。

---

## 17. 常见误解澄清

## 17.1 “这是 JWT 登录，所以服务端不保存状态”

不对。

当前实现虽然用 JWT 作为 access token，但服务端明确保存了 session，而且真正的登录态判断依赖 Redis 中的 session。

## 17.2 “access token 没过期就一定能用”

不对。

如果：

- session 已失效
- 被踢下线
- 被二次登录顶掉
- access token 版本落后

那么 access token 即使还没自然过期，也会被拒绝。

## 17.3 “refresh token 在客户端还在，就一定可以 refresh”

不对。

如果：

- refresh 已轮换
- refresh 窗口已过期
- session 已过期
- session 已被踢下线

都不能继续 refresh。

## 17.4 “踢下线就是封号”

不对。

踢下线废掉的是当前会话，不是账号本身。

账号如果仍是启用状态，重新输入账号密码仍可以再次登录。

## 17.5 “`invalidateUserSession` 会删掉这个用户所有设备的所有会话”

按当前实现，不是。

当前 Redis 只保存：

`userId -> 一个当前 sessionId`

所以它清理的是“当前唯一被承认的活跃会话”，而不是多设备列表。

---

## 18. 顺着代码读全流程时，建议这样看

如果你准备自己继续顺代码，推荐这个顺序：

1. `AuthServiceImpl.login()`
2. `LoginSessionManager.createSession()`
3. `LoginSessionManager.persistSession()`
4. `JwtTokenProvider.createAccessToken()`
5. `AuthTokenGlobalFilter.filter()`
6. `LoginSessionManager.getValidSession()`
7. `LoginSessionManager.refreshSession()`
8. `LoginSessionManager.invalidateSession()`
9. `LoginSessionManager.invalidateUserSession()`
10. `UserServiceImpl.kickout()`

这样最容易把：

- 登录
- 鉴权
- 续签
- 下线
- 顶号

串成一条完整主线。

---

## 19. 关键代码索引

前端：

- 登录页：`web-admin/src/views/login/index.vue`
- 用户会话状态：`web-admin/src/store/modules/user.js`
- token 存取：`web-admin/src/utils/auth.js`
- 请求拦截与自动 refresh：`web-admin/src/utils/request.js`
- 路由守卫：`web-admin/src/main.js`
- 用户管理踢下线入口：`web-admin/src/views/system/user/index.vue`

后端 auth：

- 登录、刷新、登出控制器：`backend/backend-auth/src/main/java/com/example/auth/controller/AuthController.java`
- profile 控制器：`backend/backend-auth/src/main/java/com/example/auth/controller/ProfileController.java`
- auth 服务实现：`backend/backend-auth/src/main/java/com/example/auth/service/impl/AuthServiceImpl.java`
- auth 服务 JWT 过滤器：`backend/backend-auth/src/main/java/com/example/auth/security/JwtAuthenticationFilter.java`

后端 common：

- session 核心：`backend/backend-common/src/main/java/com/example/common/security/LoginSessionManager.java`
- JWT 签发与解析：`backend/backend-common/src/main/java/com/example/common/security/JwtTokenProvider.java`
- session 模型：`backend/backend-common/src/main/java/com/example/common/model/security/LoginSession.java`
- 登录用户快照：`backend/backend-common/src/main/java/com/example/common/model/security/LoginUser.java`
- Redis key 常量：`backend/backend-common/src/main/java/com/example/common/constant/RedisKeyConstants.java`
- 登录用户上下文：`backend/backend-common/src/main/java/com/example/common/context/LoginUserContext.java`

后端 gateway：

- 网关鉴权过滤器：`backend/backend-gateway/src/main/java/com/example/gateway/filter/AuthTokenGlobalFilter.java`

后端 system：

- 用户控制器：`backend/backend-system/src/main/java/com/example/system/controller/UserController.java`
- 用户服务实现：`backend/backend-system/src/main/java/com/example/system/service/impl/UserServiceImpl.java`
- system 服务 JWT 过滤器：`backend/backend-system/src/main/java/com/example/system/security/JwtAuthenticationFilter.java`

---

## 20. 一句话总括

当前项目不是“只靠一个 token 证明自己在线”，而是：

- 用 `access token` 证明“我现在要访问接口”
- 用 `refresh token` 证明“我现在要续签”
- 用 `session` 证明“服务端仍然承认这是当前有效登录”

单账号单活跃会话、自动 refresh、refresh rotation、旧 access token 立刻失效、管理员踢下线，全部都是围绕同一个 `sessionId` 在运转。
