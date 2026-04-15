# Backend Manager Demo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个基于 `Vue2 + Spring Boot 2.7.x + Gateway + Spring Security + Redis + MyBatis-Plus + MySQL 8` 的可运行后台管理系统 Demo，打通登录、JWT、RBAC、动态路由、按钮权限、后端权限校验、日志记录和初始化数据。

**Architecture:** 后端采用 Maven 多模块结构，拆分为 `backend-common`、`backend-gateway`、`backend-auth`、`backend-system` 四个服务模块；前端采用 `Vue2 + Element UI` 单独工程。所有请求统一经过 `gateway`，`auth` 与 `system` 共用一套 MySQL 表结构及 Redis 在线会话，前后端基于统一权限码完成菜单、路由、按钮和接口鉴权的完整链路。

**Tech Stack:** JDK 8, Maven, Spring Boot 2.7.x, Spring Security 5.x, Spring Cloud Gateway 2021.x, MyBatis-Plus 3.5.x, MySQL 8, Redis, Vue 2, Vue Router 3, Vuex 3, Axios, Element UI 2.x

---

## File Structure

### Backend root

- Create: `backend/pom.xml`
- Create: `backend/backend-common/pom.xml`
- Create: `backend/backend-gateway/pom.xml`
- Create: `backend/backend-auth/pom.xml`
- Create: `backend/backend-system/pom.xml`

### Common module

- Create: `backend/backend-common/src/main/java/com/example/common/annotation/OperLog.java`
- Create: `backend/backend-common/src/main/java/com/example/common/aspect/OperLogAspect.java`
- Create: `backend/backend-common/src/main/java/com/example/common/config/JacksonConfig.java`
- Create: `backend/backend-common/src/main/java/com/example/common/config/MybatisPlusConfig.java`
- Create: `backend/backend-common/src/main/java/com/example/common/config/RedisConfig.java`
- Create: `backend/backend-common/src/main/java/com/example/common/constant/CommonConstants.java`
- Create: `backend/backend-common/src/main/java/com/example/common/constant/RedisKeyConstants.java`
- Create: `backend/backend-common/src/main/java/com/example/common/context/LoginUserContext.java`
- Create: `backend/backend-common/src/main/java/com/example/common/enums/MenuTypeEnum.java`
- Create: `backend/backend-common/src/main/java/com/example/common/enums/StatusEnum.java`
- Create: `backend/backend-common/src/main/java/com/example/common/exception/BusinessException.java`
- Create: `backend/backend-common/src/main/java/com/example/common/exception/GlobalExceptionHandler.java`
- Create: `backend/backend-common/src/main/java/com/example/common/model/result/ApiResult.java`
- Create: `backend/backend-common/src/main/java/com/example/common/model/result/PageResult.java`
- Create: `backend/backend-common/src/main/java/com/example/common/model/security/LoginUser.java`
- Create: `backend/backend-common/src/main/java/com/example/common/redis/RedisOperator.java`
- Create: `backend/backend-common/src/main/java/com/example/common/security/JwtTokenProvider.java`
- Create: `backend/backend-common/src/main/java/com/example/common/security/PermissionEvaluatorBean.java`
- Create: `backend/backend-common/src/main/java/com/example/common/util/IpUtils.java`
- Create: `backend/backend-common/src/main/java/com/example/common/util/PasswordUtils.java`

### Gateway module

- Create: `backend/backend-gateway/src/main/java/com/example/gateway/BackendGatewayApplication.java`
- Create: `backend/backend-gateway/src/main/java/com/example/gateway/config/CorsConfig.java`
- Create: `backend/backend-gateway/src/main/java/com/example/gateway/config/IgnoreWhiteProperties.java`
- Create: `backend/backend-gateway/src/main/java/com/example/gateway/filter/AuthTokenGlobalFilter.java`
- Create: `backend/backend-gateway/src/main/resources/application.yml`

### Auth module

- Create: `backend/backend-auth/src/main/java/com/example/auth/BackendAuthApplication.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/config/SecurityConfig.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/controller/AuthController.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/controller/ProfileController.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/dto/LoginDTO.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/dto/UpdatePasswordDTO.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/entity/SysUser.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/entity/SysRole.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/entity/SysMenu.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/entity/SysDept.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/entity/SysUserRole.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/entity/SysRoleMenu.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/entity/SysLoginLog.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/mapper/*.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/security/LoginUserDetailsService.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/security/JwtAuthenticationFilter.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/service/AuthService.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/service/impl/AuthServiceImpl.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/vo/LoginVO.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/vo/UserProfileVO.java`
- Create: `backend/backend-auth/src/main/resources/application.yml`

### System module

- Create: `backend/backend-system/src/main/java/com/example/system/BackendSystemApplication.java`
- Create: `backend/backend-system/src/main/java/com/example/system/config/SecurityConfig.java`
- Create: `backend/backend-system/src/main/java/com/example/system/security/JwtAuthenticationFilter.java`
- Create: `backend/backend-system/src/main/java/com/example/system/controller/*.java`
- Create: `backend/backend-system/src/main/java/com/example/system/dto/**/*.java`
- Create: `backend/backend-system/src/main/java/com/example/system/entity/*.java`
- Create: `backend/backend-system/src/main/java/com/example/system/mapper/*.java`
- Create: `backend/backend-system/src/main/java/com/example/system/service/*.java`
- Create: `backend/backend-system/src/main/java/com/example/system/service/impl/*.java`
- Create: `backend/backend-system/src/main/java/com/example/system/vo/**/*.java`
- Create: `backend/backend-system/src/main/resources/application.yml`
- Create: `backend/backend-system/src/main/resources/mapper/*.xml`

### SQL and docs

- Create: `backend/sql/schema.sql`
- Create: `backend/sql/data.sql`
- Create: `README.md`

### Frontend

- Create: `web-admin/package.json`
- Create: `web-admin/vue.config.js`
- Create: `web-admin/.env.development`
- Create: `web-admin/public/index.html`
- Create: `web-admin/src/main.js`
- Create: `web-admin/src/App.vue`
- Create: `web-admin/src/api/*.js`
- Create: `web-admin/src/directive/permission.js`
- Create: `web-admin/src/layout/**/*.vue`
- Create: `web-admin/src/router/index.js`
- Create: `web-admin/src/router/modules/asyncRoutes.js`
- Create: `web-admin/src/store/index.js`
- Create: `web-admin/src/store/modules/*.js`
- Create: `web-admin/src/utils/request.js`
- Create: `web-admin/src/utils/auth.js`
- Create: `web-admin/src/utils/route.js`
- Create: `web-admin/src/views/login/index.vue`
- Create: `web-admin/src/views/dashboard/index.vue`
- Create: `web-admin/src/views/system/user/index.vue`
- Create: `web-admin/src/views/system/role/index.vue`
- Create: `web-admin/src/views/system/menu/index.vue`
- Create: `web-admin/src/views/system/dept/index.vue`
- Create: `web-admin/src/views/log/login/index.vue`
- Create: `web-admin/src/views/log/operation/index.vue`
- Create: `web-admin/src/views/profile/index.vue`
- Create: `web-admin/src/views/error/403.vue`
- Create: `web-admin/src/views/error/404.vue`

## Task 1: Initialize backend parent project and dependency management

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/backend-common/pom.xml`
- Create: `backend/backend-gateway/pom.xml`
- Create: `backend/backend-auth/pom.xml`
- Create: `backend/backend-system/pom.xml`

- [ ] **Step 1: Write the failing verification command**

```powershell
mvn -f backend/pom.xml -q validate
```

Expected: FAIL with “Non-readable POM” or “The system cannot find the path specified”

- [ ] **Step 2: Create parent POM with UTF-8, JDK8, dependency management**

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>backend-manager</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <modules>
        <module>backend-common</module>
        <module>backend-gateway</module>
        <module>backend-auth</module>
        <module>backend-system</module>
    </modules>

    <properties>
        <java.version>1.8</java.version>
        <spring.boot.version>2.7.18</spring.boot.version>
        <spring.cloud.version>2021.0.8</spring.cloud.version>
        <mybatis.plus.version>3.5.5</mybatis.plus.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <maven.compiler.encoding>UTF-8</maven.compiler.encoding>
    </properties>
</project>
```

- [ ] **Step 3: Add child module POM skeletons**

```xml
<parent>
    <groupId>com.example</groupId>
    <artifactId>backend-manager</artifactId>
    <version>1.0.0</version>
</parent>
<artifactId>backend-common</artifactId>
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

- [ ] **Step 4: Run validate to verify POM structure is correct**

```powershell
mvn -f backend/pom.xml -q validate
```

Expected: PASS with no build error

- [ ] **Step 5: Commit**

```bash
git add backend/pom.xml backend/backend-common/pom.xml backend/backend-gateway/pom.xml backend/backend-auth/pom.xml backend/backend-system/pom.xml
git commit -m "build: initialize backend multi-module pom"
```

## Task 2: Build common module infrastructure

**Files:**
- Create: `backend/backend-common/src/main/java/com/example/common/model/result/ApiResult.java`
- Create: `backend/backend-common/src/main/java/com/example/common/model/result/PageResult.java`
- Create: `backend/backend-common/src/main/java/com/example/common/exception/BusinessException.java`
- Create: `backend/backend-common/src/main/java/com/example/common/exception/GlobalExceptionHandler.java`
- Create: `backend/backend-common/src/main/java/com/example/common/security/JwtTokenProvider.java`
- Create: `backend/backend-common/src/main/java/com/example/common/redis/RedisOperator.java`
- Create: `backend/backend-common/src/main/java/com/example/common/model/security/LoginUser.java`
- Create: `backend/backend-common/src/main/java/com/example/common/security/PermissionEvaluatorBean.java`
- Create: `backend/backend-common/src/main/java/com/example/common/annotation/OperLog.java`
- Create: `backend/backend-common/src/main/java/com/example/common/aspect/OperLogAspect.java`

- [ ] **Step 1: Write a failing common-module compile check**

```powershell
mvn -f backend/pom.xml -pl backend-common -q test
```

Expected: FAIL because common source files do not exist

- [ ] **Step 2: Implement unified response and exception classes**

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {
    private Integer code;
    private String message;
    private Boolean success;
    private T data;

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", true, data);
    }

    public static <T> ApiResult<T> fail(Integer code, String message) {
        return new ApiResult<>(code, message, false, null);
    }
}
```

- [ ] **Step 3: Implement JWT and Redis helpers**

```java
public String createToken(LoginUser loginUser) {
    Date now = new Date();
    Date expireAt = new Date(now.getTime() + expireMillis);
    return Jwts.builder()
            .setSubject(String.valueOf(loginUser.getUserId()))
            .claim("username", loginUser.getUsername())
            .claim("roles", loginUser.getRoles())
            .claim("permissions", loginUser.getPermissions())
            .setIssuedAt(now)
            .setExpiration(expireAt)
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
}
```

- [ ] **Step 4: Implement permission bean and operation-log annotation**

```java
@Component("perm")
public class PermissionEvaluatorBean {
    public boolean hasPermission(String permissionCode) {
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            return false;
        }
        if (loginUser.isSuperAdmin()) {
            return true;
        }
        return loginUser.getPermissions() != null && loginUser.getPermissions().contains(permissionCode);
    }
}
```

- [ ] **Step 5: Run common-module tests/compile**

```powershell
mvn -f backend/pom.xml -pl backend-common -q test
```

Expected: PASS or zero-test success with successful compilation

- [ ] **Step 6: Commit**

```bash
git add backend/backend-common
git commit -m "feat: add common infrastructure module"
```

## Task 3: Add gateway with static routes and Redis token validation

**Files:**
- Create: `backend/backend-gateway/src/main/java/com/example/gateway/BackendGatewayApplication.java`
- Create: `backend/backend-gateway/src/main/java/com/example/gateway/config/CorsConfig.java`
- Create: `backend/backend-gateway/src/main/java/com/example/gateway/config/IgnoreWhiteProperties.java`
- Create: `backend/backend-gateway/src/main/java/com/example/gateway/filter/AuthTokenGlobalFilter.java`
- Create: `backend/backend-gateway/src/main/resources/application.yml`

- [ ] **Step 1: Write a failing gateway boot command**

```powershell
mvn -f backend/pom.xml -pl backend-gateway spring-boot:run
```

Expected: FAIL because gateway application class and configuration do not exist

- [ ] **Step 2: Create gateway application and white-list configuration**

```java
@SpringBootApplication(scanBasePackages = "com.example")
@EnableConfigurationProperties(IgnoreWhiteProperties.class)
public class BackendGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendGatewayApplication.class, args);
    }
}
```

```yaml
server:
  port: 8080
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://127.0.0.1:9200
          predicates:
            - Path=/api/auth/**
        - id: system-service
          uri: http://127.0.0.1:9300
          predicates:
            - Path=/api/system/**
```

- [ ] **Step 3: Implement token parsing and Redis online validation**

```java
if (ignoreWhiteProperties.match(path)) {
    return chain.filter(exchange);
}
String token = resolveToken(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
if (!StringUtils.hasText(token) || !jwtTokenProvider.validateToken(token)) {
    return writeUnauthorized(exchange, "登录状态已失效，请重新登录");
}
if (!redisOperator.hasKey(RedisKeyConstants.loginTokenKey(token))) {
    return writeUnauthorized(exchange, "Token 已失效或已退出登录");
}
```

- [ ] **Step 4: Add CORS and UTF-8-friendly configuration comments**

```yaml
spring:
  redis:
    host: 127.0.0.1 # 中文注释：这里改成你的 Redis 地址
    port: 6379      # 中文注释：这里改成你的 Redis 端口
    password:       # 中文注释：如果 Redis 有密码，请在这里填写
```

- [ ] **Step 5: Run gateway module compile**

```powershell
mvn -f backend/pom.xml -pl backend-gateway -q test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/backend-gateway
git commit -m "feat: add gateway entry and token validation"
```

## Task 4: Implement auth service login/profile/logout chain

**Files:**
- Create: `backend/backend-auth/src/main/java/com/example/auth/BackendAuthApplication.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/config/SecurityConfig.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/controller/AuthController.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/controller/ProfileController.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/dto/LoginDTO.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/dto/UpdatePasswordDTO.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/entity/*.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/mapper/*.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/security/LoginUserDetailsService.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/security/JwtAuthenticationFilter.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/service/AuthService.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/service/impl/AuthServiceImpl.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/vo/LoginVO.java`
- Create: `backend/backend-auth/src/main/java/com/example/auth/vo/UserProfileVO.java`
- Create: `backend/backend-auth/src/main/resources/application.yml`

- [ ] **Step 1: Write a failing auth login test/boot check**

```powershell
mvn -f backend/pom.xml -pl backend-auth -q test
```

Expected: FAIL because auth module classes and mapper configuration are missing

- [ ] **Step 2: Add entities, mappers, and login service**

```java
public LoginVO login(LoginDTO dto, HttpServletRequest request) {
    SysUser user = userMapper.selectByUsername(dto.getUsername());
    if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
        loginLogService.recordFail(dto.getUsername(), request, "用户不存在");
        throw new BusinessException("用户名或密码错误");
    }
    if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
        loginLogService.recordFail(dto.getUsername(), request, "密码错误");
        throw new BusinessException("用户名或密码错误");
    }
    LoginUser loginUser = buildLoginUser(user);
    String token = jwtTokenProvider.createToken(loginUser);
    cacheLoginToken(token, loginUser);
    loginLogService.recordSuccess(user, request);
    return new LoginVO(token, "Bearer", jwtTokenProvider.getExpireSeconds());
}
```

- [ ] **Step 3: Add profile API that returns user, roles, permissions, menus**

```java
@GetMapping("/user/profile")
public ApiResult<UserProfileVO> profile() {
    return ApiResult.success(authService.getCurrentUserProfile());
}
```

- [ ] **Step 4: Add logout and update-password logic**

```java
public void logout(String token) {
    redisOperator.delete(RedisKeyConstants.loginTokenKey(token));
}
```

- [ ] **Step 5: Run auth-module compile**

```powershell
mvn -f backend/pom.xml -pl backend-auth -q test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/backend-auth
git commit -m "feat: implement auth service login and profile APIs"
```

## Task 5: Implement system module entities, mappers, and security baseline

**Files:**
- Create: `backend/backend-system/src/main/java/com/example/system/BackendSystemApplication.java`
- Create: `backend/backend-system/src/main/java/com/example/system/config/SecurityConfig.java`
- Create: `backend/backend-system/src/main/java/com/example/system/security/JwtAuthenticationFilter.java`
- Create: `backend/backend-system/src/main/java/com/example/system/entity/*.java`
- Create: `backend/backend-system/src/main/java/com/example/system/mapper/*.java`
- Create: `backend/backend-system/src/main/resources/application.yml`

- [ ] **Step 1: Write a failing system-module compile command**

```powershell
mvn -f backend/pom.xml -pl backend-system -q test
```

Expected: FAIL because system module does not exist yet

- [ ] **Step 2: Add system boot class, datasource, redis, MyBatis-Plus config**

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/backend_manager?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root
  redis:
    host: 127.0.0.1 # 中文注释：这里改成你的 Redis 地址
    port: 6379      # 中文注释：这里改成你的 Redis 端口
```

- [ ] **Step 3: Add JWT filter and stateless security configuration**

```java
http.csrf().disable()
    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    .and()
    .authorizeHttpRequests(auth -> auth
        .antMatchers("/error").permitAll()
        .anyRequest().authenticated())
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

- [ ] **Step 4: Run system-module compile**

```powershell
mvn -f backend/pom.xml -pl backend-system -q test
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/backend-system
git commit -m "feat: initialize system service security baseline"
```

## Task 6: Implement dashboard, department, and menu tree APIs

**Files:**
- Create: `backend/backend-system/src/main/java/com/example/system/controller/DashboardController.java`
- Create: `backend/backend-system/src/main/java/com/example/system/controller/DeptController.java`
- Create: `backend/backend-system/src/main/java/com/example/system/controller/MenuController.java`
- Create: `backend/backend-system/src/main/java/com/example/system/dto/dept/*.java`
- Create: `backend/backend-system/src/main/java/com/example/system/dto/menu/*.java`
- Create: `backend/backend-system/src/main/java/com/example/system/service/DashboardService.java`
- Create: `backend/backend-system/src/main/java/com/example/system/service/DeptService.java`
- Create: `backend/backend-system/src/main/java/com/example/system/service/MenuService.java`
- Create: `backend/backend-system/src/main/java/com/example/system/service/impl/*.java`
- Create: `backend/backend-system/src/main/java/com/example/system/vo/dashboard/*.java`
- Create: `backend/backend-system/src/main/java/com/example/system/vo/dept/*.java`
- Create: `backend/backend-system/src/main/java/com/example/system/vo/menu/*.java`

- [ ] **Step 1: Write failing endpoint verification commands**

```powershell
mvn -f backend/pom.xml -pl backend-system -q test
```

Expected: FAIL because controllers and services are not implemented

- [ ] **Step 2: Implement dashboard overview service**

```java
public DashboardOverviewVO overview() {
    DashboardOverviewVO vo = new DashboardOverviewVO();
    vo.setUserCount(userMapper.selectCount(null));
    vo.setRoleCount(roleMapper.selectCount(null));
    vo.setMenuCount(menuMapper.selectCount(null));
    vo.setRecentLoginCount(loginLogMapper.countRecentSevenDays());
    vo.setRecentLoginLogs(loginLogMapper.selectRecentList(8));
    return vo;
}
```

- [ ] **Step 3: Implement dept tree and menu tree CRUD**

```java
@PreAuthorize("@perm.hasPermission('system:dept:list')")
@GetMapping("/tree")
public ApiResult<List<DeptTreeVO>> tree() {
    return ApiResult.success(deptService.treeList());
}
```

- [ ] **Step 4: Add operation log annotations to write operations**

```java
@OperLog(module = "菜单管理", type = "新增")
@PreAuthorize("@perm.hasPermission('system:menu:add')")
@PostMapping
public ApiResult<Void> save(@Validated @RequestBody MenuSaveDTO dto) {
    menuService.saveMenu(dto);
    return ApiResult.success(null);
}
```

- [ ] **Step 5: Run system tests/compile**

```powershell
mvn -f backend/pom.xml -pl backend-system -q test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/backend-system
git commit -m "feat: add dashboard dept and menu APIs"
```

## Task 7: Implement user and role management APIs with RBAC assignment

**Files:**
- Create: `backend/backend-system/src/main/java/com/example/system/controller/UserController.java`
- Create: `backend/backend-system/src/main/java/com/example/system/controller/RoleController.java`
- Create: `backend/backend-system/src/main/java/com/example/system/dto/user/*.java`
- Create: `backend/backend-system/src/main/java/com/example/system/dto/role/*.java`
- Create: `backend/backend-system/src/main/java/com/example/system/service/UserService.java`
- Create: `backend/backend-system/src/main/java/com/example/system/service/RoleService.java`
- Create: `backend/backend-system/src/main/java/com/example/system/service/impl/UserServiceImpl.java`
- Create: `backend/backend-system/src/main/java/com/example/system/service/impl/RoleServiceImpl.java`
- Create: `backend/backend-system/src/main/java/com/example/system/vo/user/*.java`
- Create: `backend/backend-system/src/main/java/com/example/system/vo/role/*.java`

- [ ] **Step 1: Write failing compile command for user/role features**

```powershell
mvn -f backend/pom.xml -pl backend-system -q test
```

Expected: FAIL until user and role controllers/services compile

- [ ] **Step 2: Implement user pagination, create, edit, status, reset-password, assign-role**

```java
@PreAuthorize("@perm.hasPermission('system:user:list')")
@GetMapping("/page")
public ApiResult<PageResult<UserPageVO>> page(UserQueryDTO dto) {
    return ApiResult.success(userService.pageQuery(dto));
}
```

```java
@OperLog(module = "用户管理", type = "分配角色")
@PreAuthorize("@perm.hasPermission('system:user:assign-role')")
@PutMapping("/{id}/roles")
public ApiResult<Void> assignRoles(@PathVariable Long id, @RequestBody UserRoleAssignDTO dto) {
    userService.assignRoles(id, dto.getRoleIds());
    return ApiResult.success(null);
}
```

- [ ] **Step 3: Implement role pagination, create, edit, status, assign-menu**

```java
@OperLog(module = "角色管理", type = "分配权限")
@PreAuthorize("@perm.hasPermission('system:role:assign')")
@PutMapping("/{id}/menus")
public ApiResult<Void> assignMenus(@PathVariable Long id, @RequestBody RoleMenuAssignDTO dto) {
    roleService.assignMenus(id, dto.getMenuIds());
    return ApiResult.success(null);
}
```

- [ ] **Step 4: Ensure 401 and 403 are returned distinctly**

```java
http.exceptionHandling()
    .authenticationEntryPoint((request, response, ex) -> writeJson(response, 401, "未登录或登录状态已过期"))
    .accessDeniedHandler((request, response, ex) -> writeJson(response, 403, "没有访问该资源的权限"));
```

- [ ] **Step 5: Run system-module compile**

```powershell
mvn -f backend/pom.xml -pl backend-system -q test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/backend-system
git commit -m "feat: add user and role management APIs"
```

## Task 8: Implement login-log and operation-log query APIs

**Files:**
- Create: `backend/backend-system/src/main/java/com/example/system/controller/LoginLogController.java`
- Create: `backend/backend-system/src/main/java/com/example/system/controller/OperationLogController.java`
- Create: `backend/backend-system/src/main/java/com/example/system/dto/log/*.java`
- Create: `backend/backend-system/src/main/java/com/example/system/service/LoginLogService.java`
- Create: `backend/backend-system/src/main/java/com/example/system/service/OperationLogService.java`
- Create: `backend/backend-system/src/main/java/com/example/system/service/impl/LoginLogServiceImpl.java`
- Create: `backend/backend-system/src/main/java/com/example/system/service/impl/OperationLogServiceImpl.java`
- Create: `backend/backend-system/src/main/java/com/example/system/vo/log/*.java`

- [ ] **Step 1: Write failing compile command**

```powershell
mvn -f backend/pom.xml -pl backend-system -q test
```

Expected: FAIL until log query services exist

- [ ] **Step 2: Implement login-log pagination**

```java
@PreAuthorize("@perm.hasPermission('system:login-log:list')")
@GetMapping("/page")
public ApiResult<PageResult<LoginLogPageVO>> page(LoginLogQueryDTO dto) {
    return ApiResult.success(loginLogService.pageQuery(dto));
}
```

- [ ] **Step 3: Implement operation-log pagination**

```java
@PreAuthorize("@perm.hasPermission('system:operation-log:list')")
@GetMapping("/page")
public ApiResult<PageResult<OperationLogPageVO>> page(OperationLogQueryDTO dto) {
    return ApiResult.success(operationLogService.pageQuery(dto));
}
```

- [ ] **Step 4: Run system-module compile**

```powershell
mvn -f backend/pom.xml -pl backend-system -q test
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/backend-system
git commit -m "feat: add login and operation log APIs"
```

## Task 9: Add schema.sql and data.sql with full RBAC seed data

**Files:**
- Create: `backend/sql/schema.sql`
- Create: `backend/sql/data.sql`

- [ ] **Step 1: Write failing import expectation**

```sql
SOURCE backend/sql/schema.sql;
SOURCE backend/sql/data.sql;
```

Expected: FAIL because SQL files do not exist

- [ ] **Step 2: Create schema for all required tables**

```sql
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `dept_id` BIGINT DEFAULT NULL,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `nickname` VARCHAR(64) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

- [ ] **Step 3: Create seed data for users, roles, menus, departments, logs**

```sql
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `status`) VALUES
(1, '超级管理员', 'SUPER_ADMIN', 1),
(2, '系统管理员', 'SYSTEM_ADMIN', 1),
(3, '运营角色', 'OPERATOR', 1),
(4, '审计角色', 'AUDITOR', 1);
```

- [ ] **Step 4: Verify SQL imports cleanly into MySQL 8**

```powershell
mysql -uroot -proot -e "DROP DATABASE IF EXISTS backend_manager; CREATE DATABASE backend_manager DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; USE backend_manager; SOURCE backend/sql/schema.sql; SOURCE backend/sql/data.sql;"
```

Expected: PASS with no SQL syntax error

- [ ] **Step 5: Commit**

```bash
git add backend/sql/schema.sql backend/sql/data.sql
git commit -m "feat: add mysql schema and seed data"
```

## Task 10: Initialize Vue2 frontend scaffold and base application shell

**Files:**
- Create: `web-admin/package.json`
- Create: `web-admin/vue.config.js`
- Create: `web-admin/.env.development`
- Create: `web-admin/public/index.html`
- Create: `web-admin/src/main.js`
- Create: `web-admin/src/App.vue`
- Create: `web-admin/src/layout/**/*.vue`
- Create: `web-admin/src/router/index.js`
- Create: `web-admin/src/store/index.js`
- Create: `web-admin/src/utils/request.js`
- Create: `web-admin/src/utils/auth.js`

- [ ] **Step 1: Write failing frontend install/build command**

```powershell
cd web-admin
npm install
npm run build
```

Expected: FAIL because frontend project does not exist

- [ ] **Step 2: Create package.json and Vue CLI-compatible build config**

```json
{
  "name": "web-admin",
  "private": true,
  "scripts": {
    "serve": "vue-cli-service serve",
    "build": "vue-cli-service build"
  }
}
```

- [ ] **Step 3: Create main entry, router, store, request interceptor**

```js
axios.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
```

- [ ] **Step 4: Build base layout with sidebar, navbar, breadcrumb**

```vue
<template>
  <div class="layout-shell">
    <sidebar-menu />
    <div class="layout-main">
      <navbar />
      <breadcrumb />
      <app-main />
    </div>
  </div>
</template>
```

- [ ] **Step 5: Run frontend build**

```powershell
cd web-admin
npm run build
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add web-admin
git commit -m "feat: initialize vue2 admin shell"
```

## Task 11: Implement login, store modules, dynamic routes, and permission directive

**Files:**
- Create: `web-admin/src/api/auth.js`
- Create: `web-admin/src/directive/permission.js`
- Create: `web-admin/src/router/modules/asyncRoutes.js`
- Create: `web-admin/src/store/modules/user.js`
- Create: `web-admin/src/store/modules/permission.js`
- Create: `web-admin/src/views/login/index.vue`
- Modify: `web-admin/src/router/index.js`
- Modify: `web-admin/src/main.js`

- [ ] **Step 1: Write failing frontend build**

```powershell
cd web-admin
npm run build
```

Expected: FAIL because login page, store modules, or route transform code are missing

- [ ] **Step 2: Implement login page and user store**

```js
async login({ commit, dispatch }, form) {
  const { data } = await loginApi(form)
  setToken(data.accessToken)
  commit('SET_TOKEN', data.accessToken)
  await dispatch('fetchProfile')
}
```

- [ ] **Step 3: Implement menu-tree to route transform**

```js
export function buildAsyncRoutes(menus) {
  return menus
    .filter(item => item.menuType !== 'BUTTON' && item.visible === 1 && item.status === 1)
    .map(item => mapMenuToRoute(item))
}
```

- [ ] **Step 4: Implement v-permission directive**

```js
export default {
  inserted(el, binding) {
    const permissions = store.getters.permissions
    const value = binding.value
    if (!permissions.includes(value) && !store.getters.isSuperAdmin) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}
```

- [ ] **Step 5: Run frontend build**

```powershell
cd web-admin
npm run build
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add web-admin
git commit -m "feat: add login flow dynamic routes and permission directive"
```

## Task 12: Implement dashboard, user, role, menu, dept, log, profile, and error pages

**Files:**
- Create: `web-admin/src/api/dashboard.js`
- Create: `web-admin/src/api/user.js`
- Create: `web-admin/src/api/role.js`
- Create: `web-admin/src/api/menu.js`
- Create: `web-admin/src/api/dept.js`
- Create: `web-admin/src/api/log.js`
- Create: `web-admin/src/views/dashboard/index.vue`
- Create: `web-admin/src/views/system/user/index.vue`
- Create: `web-admin/src/views/system/role/index.vue`
- Create: `web-admin/src/views/system/menu/index.vue`
- Create: `web-admin/src/views/system/dept/index.vue`
- Create: `web-admin/src/views/log/login/index.vue`
- Create: `web-admin/src/views/log/operation/index.vue`
- Create: `web-admin/src/views/profile/index.vue`
- Create: `web-admin/src/views/error/403.vue`
- Create: `web-admin/src/views/error/404.vue`

- [ ] **Step 1: Write failing build**

```powershell
cd web-admin
npm run build
```

Expected: FAIL until all view components and APIs exist

- [ ] **Step 2: Implement dashboard and profile pages**

```vue
<el-row :gutter="16">
  <el-col :span="6" v-for="card in cards" :key="card.label">
    <el-card class="stat-card">
      <div class="stat-value">{{ card.value }}</div>
      <div class="stat-label">{{ card.label }}</div>
    </el-card>
  </el-col>
</el-row>
```

- [ ] **Step 3: Implement user, role, menu, dept pages with query area, table/tree, dialog, pagination**

```vue
<el-button v-permission="'system:user:add'" type="primary" @click="openCreate">新增用户</el-button>
<el-table :data="tableData" border stripe v-loading="loading">...</el-table>
<el-pagination :current-page.sync="query.current" :page-size.sync="query.size" :total="total" @current-change="fetchList" />
```

- [ ] **Step 4: Implement log pages and 403/404 pages**

```vue
<template>
  <div class="error-page">
    <h1>403</h1>
    <p>抱歉，当前账号没有访问此页面的权限。</p>
  </div>
</template>
```

- [ ] **Step 5: Run frontend build**

```powershell
cd web-admin
npm run build
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add web-admin
git commit -m "feat: add admin pages and log views"
```

## Task 13: Write README and perform full-stack verification

**Files:**
- Create: `README.md`
- Modify: `backend/backend-gateway/src/main/resources/application.yml`
- Modify: `backend/backend-auth/src/main/resources/application.yml`
- Modify: `backend/backend-system/src/main/resources/application.yml`
- Verify: `backend/sql/schema.sql`
- Verify: `backend/sql/data.sql`

- [ ] **Step 1: Write failing end-to-end verification checklist**

```powershell
mvn -f backend/pom.xml clean package
cd web-admin
npm run build
```

Expected: FAIL until all backend/frontend pieces are complete

- [ ] **Step 2: Write README with startup steps, accounts, routes, permission explanation**

```md
## 默认账号

- admin / Admin@123456
- sysadmin / Admin@123456
- operator / Admin@123456
- auditor / Admin@123456
```

- [ ] **Step 3: Start backend services and verify endpoints**

```powershell
mvn -f backend/pom.xml clean package
mvn -f backend/backend-auth/pom.xml spring-boot:run
mvn -f backend/backend-system/pom.xml spring-boot:run
mvn -f backend/backend-gateway/pom.xml spring-boot:run
```

Expected: all three services start successfully on `9200`, `9300`, `8080`

- [ ] **Step 4: Start frontend and verify login + routing + permission flow**

```powershell
cd web-admin
npm install
npm run serve
```

Expected: open `http://localhost:8081`, login works, menus differ by role, unauthorized page returns 403 behavior

- [ ] **Step 5: Commit**

```bash
git add README.md backend web-admin
git commit -m "docs: add startup guide and verify full demo"
```

## Self-Review

### Spec coverage

- 统一网关入口：Task 3
- 登录认证/JWT/Redis 在线会话：Task 4、Task 5
- RBAC 与后端权限校验：Task 4、Task 7
- 菜单/路由/按钮联动：Task 6、Task 11、Task 12
- 用户/角色/菜单/部门/日志模块：Task 6、Task 7、Task 8、Task 12
- SQL 与初始化数据：Task 9
- README 和启动方式：Task 13
- UTF-8 编码与中文注释：Task 1、Task 3、Task 5、Task 13

### Placeholder scan

- 无 `TODO`
- 无 `TBD`
- 无“后续补充实现”式占位步骤

### Type consistency

- 统一使用 `ApiResult` / `PageResult`
- 统一权限判断入口为 `@perm.hasPermission(...)`
- 统一 token 前缀为 `Bearer`
- 统一菜单类型为 `CATALOG` / `MENU` / `BUTTON`
