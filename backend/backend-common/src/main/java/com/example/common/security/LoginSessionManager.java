package com.example.common.security;

import com.example.common.constant.CommonConstants;
import com.example.common.constant.RedisKeyConstants;
import com.example.common.exception.BusinessException;
import com.example.common.model.security.AuthTokens;
import com.example.common.model.security.LoginSession;
import com.example.common.model.security.LoginUser;
import com.example.common.redis.RedisOperator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * 登录会话管理器
 * 负责创建、刷新、验证和管理用户的登录会话
 */
@Component
public class LoginSessionManager {

    // 安全随机数生成器，用于生成令牌
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisOperator redisOperator;

    // 刷新令牌过期时间（默认7天）
    @Value("${security.jwt.refresh-expire-seconds:604800}")
    private long refreshExpireSeconds;

    // 刷新令牌最大过期时间（默认30天）
    @Value("${security.jwt.refresh-max-expire-seconds:2592000}")
    private long refreshMaxExpireSeconds;

    /**
     * 构造函数
     * @param jwtTokenProvider JWT令牌提供者
     * @param redisOperator Redis操作器
     */
    public LoginSessionManager(JwtTokenProvider jwtTokenProvider, RedisOperator redisOperator) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisOperator = redisOperator;
    }

/**
 * 创建用户会话并生成认证令牌
 * @param loginUser 登录用户信息
 * @return 返回包含访问令牌和刷新令牌的AuthTokens对象
 */
    public AuthTokens createSession(LoginUser loginUser) {
    // 使该用户现有的会话失效
        invalidateUserSession(loginUser.getUserId());  // 确保用户只有一个活跃的会话
    // 获取当前时间戳
        long now = Instant.now().toEpochMilli();     // 获取当前时间的毫秒值
    // 计算会话过期时间
        long sessionExpireAt = now + effectiveRefreshMaxExpireSeconds() * 1000;  // 计算会话过期时间
        long refreshExpireAt = Math.min(now + refreshExpireSeconds * 1000, sessionExpireAt);  // 计算刷新令牌过期时间，取最小值确保不会超过会话过期时间
        String sessionId = generateOpaqueToken();     // 生成不透明的会话ID
        String refreshToken = generateOpaqueToken();  // 生成不透明的刷新令牌
        String refreshTokenHash = sha256(refreshToken);  // 对刷新令牌进行哈希处理

        LoginSession session = new LoginSession();     // 创建新的登录会话对象
        session.setSessionId(sessionId);        // 设置会话ID
        session.setUserId(loginUser.getUserId()); //    设置用户ID
        session.setLoginUser(loginUser);             // 设置登录用户信息
        session.setRefreshTokenHash(refreshTokenHash); // 设置刷新令牌的哈希值
        session.setAccessTokenVersion(1);             // 设置访问令牌版本
        session.setRefreshExpireAt(refreshExpireAt);  // 设置刷新令牌过期时间
        session.setSessionExpireAt(sessionExpireAt);   // 设置会话过期时间
        session.setLastRefreshAt(now);               // 设置最后刷新时间

        persistSession(session, refreshTokenHash);   // 持久化会话信息
        return toAuthTokens(session, refreshToken);   // 返回包含访问令牌和刷新令牌的AuthTokens对象
    }

    /**
     * 刷新用户会话
     * @param refreshToken 刷新令牌
     * @return 返回新的认证令牌
     */
    public AuthTokens refreshSession(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw unauthorized("Refresh token 已失效，请重新登录");
        }

        String refreshTokenHash = sha256(refreshToken);
        String sessionId = redisOperator.get(RedisKeyConstants.loginRefreshKey(refreshTokenHash), String.class);
        if (!StringUtils.hasText(sessionId)) {
            throw unauthorized("Refresh token 已失效，请重新登录");
        }

        LoginSession session = redisOperator.get(RedisKeyConstants.loginSessionKey(sessionId), LoginSession.class);
        if (!isSessionOnline(session, sessionId) || !refreshTokenHash.equals(session.getRefreshTokenHash())) {
            invalidateSession(sessionId);
            throw unauthorized("登录状态已失效，请重新登录");
        }

        long now = Instant.now().toEpochMilli();
        if (session.getRefreshExpireAt() == null || session.getRefreshExpireAt() <= now
                || session.getSessionExpireAt() == null || session.getSessionExpireAt() <= now) {
            invalidateSession(sessionId);
            throw unauthorized("登录状态已失效，请重新登录");
        }

        String newRefreshToken = generateOpaqueToken();
        String newRefreshTokenHash = sha256(newRefreshToken);
        String oldRefreshTokenHash = session.getRefreshTokenHash();
        long nextRefreshExpireAt = Math.min(now + refreshExpireSeconds * 1000, session.getSessionExpireAt());

        session.setRefreshTokenHash(newRefreshTokenHash);
        session.setAccessTokenVersion(session.getAccessTokenVersion() + 1);
        session.setRefreshExpireAt(nextRefreshExpireAt);
        session.setLastRefreshAt(now);

        redisOperator.delete(RedisKeyConstants.loginRefreshKey(oldRefreshTokenHash));
        persistSession(session, newRefreshTokenHash);
        return toAuthTokens(session, newRefreshToken);
    }

/**
 * 获取有效的登录会话
 * @param accessToken 访问令牌
 * @return 如果验证通过返回有效的LoginSession对象，否则返回null
 */
    public LoginSession getValidSession(String accessToken) {
        // 验证访问令牌的有效性
        if (!jwtTokenProvider.validateAccessToken(accessToken)) {
            return null;
        }
        // 从访问令牌中获取会话ID
        String sessionId = jwtTokenProvider.getSessionId(accessToken);
        // 从Redis中获取登录会话信息
        LoginSession session = redisOperator.get(RedisKeyConstants.loginSessionKey(sessionId), LoginSession.class);
        // 检查会话是否在线
        if (!isSessionOnline(session, sessionId)) {
            return null;
        }
        // 获取并验证访问令牌版本
        Integer accessTokenVersion = jwtTokenProvider.getAccessTokenVersion(accessToken);
        if (accessTokenVersion == null || !accessTokenVersion.equals(session.getAccessTokenVersion())) {
            return null;
        }
        // 验证访问令牌中的用户ID与会话中的用户ID是否一致
        if (!jwtTokenProvider.getUserId(accessToken).equals(session.getUserId())) {
            return null;
        }
        return session;
    }

    /**
     * 使访问令牌失效
     * @param accessToken 访问令牌
     */
    public void invalidateAccessToken(String accessToken) {
        if (!jwtTokenProvider.validateAccessToken(accessToken)) {
            return;
        }
        invalidateSession(jwtTokenProvider.getSessionId(accessToken));
    }

    /**
     * 使用户的所有会话失效
     * @param userId 用户ID
     */
    public void invalidateUserSession(Long userId) {
    // 检查userId是否为null，如果是则直接返回
        if (userId == null) {
            return;
        }
    // 从Redis中获取用户对应的sessionId
        String sessionId = redisOperator.get(RedisKeyConstants.loginUserKey(userId), String.class);
    // 如果sessionId存在且不为空，则使该会话失效
        if (StringUtils.hasText(sessionId)) {
            invalidateSession(sessionId);
        } else {
        // 如果不存在sessionId，则直接删除Redis中对应的用户键
            redisOperator.delete(RedisKeyConstants.loginUserKey(userId));
        }
    }

    /**
     * 将登录会话转换为认证令牌
     * @param session 登录会话
     * @param refreshToken 刷新令牌
     * @return 认证令牌对象
     */
    private AuthTokens toAuthTokens(LoginSession session, String refreshToken) {
        String accessToken = jwtTokenProvider.createAccessToken(
                session.getLoginUser(),
                session.getSessionId(),
                session.getAccessTokenVersion()
        );
        long now = Instant.now().toEpochMilli();
        long refreshExpireIn = Math.max(1L, (session.getRefreshExpireAt() - now) / 1000);
        return new AuthTokens(
                accessToken,
                refreshToken,
                CommonConstants.TOKEN_PREFIX.trim(),
                jwtTokenProvider.getAccessExpireSeconds(),
                refreshExpireIn
        );
    }

    /**
     * 持久化登录会话到Redis
     * @param session 登录会话
     * @param refreshTokenHash 刷新令牌哈希
     */
    /**
     * 持久化登录会话信息到Redis中
     * @param session 登录会话对象，包含用户会话相关信息
     * @param refreshTokenHash 刷新令牌的哈希值，用于后续刷新会话使用
     */
    private void persistSession(LoginSession session, String refreshTokenHash) {
        // 获取当前时间戳（毫秒）
        long now = Instant.now().toEpochMilli();
        // 计算会话存活时间（秒），确保至少为1秒
        long sessionTtlSeconds = Math.max(1L, (session.getSessionExpireAt() - now) / 1000);
        // 计算刷新令牌存活时间（秒），确保至少为1秒
        long refreshTtlSeconds = Math.max(1L, (session.getRefreshExpireAt() - now) / 1000);

    /**
     * 使会话失效
     * @param sessionId 会话ID
     */
        // 将用户ID与会话ID的映射关系存入Redis，设置会话过期时间
        redisOperator.set(RedisKeyConstants.loginUserKey(session.getUserId()), session.getSessionId(),
                sessionTtlSeconds, TimeUnit.SECONDS);
        // 将会话ID与完整会话信息的映射关系存入Redis，设置会话过期时间
        redisOperator.set(RedisKeyConstants.loginSessionKey(session.getSessionId()), session,
                sessionTtlSeconds, TimeUnit.SECONDS);
        // 将刷新令牌哈希与会话ID的映射关系存入Redis，设置刷新令牌过期时间
        redisOperator.set(RedisKeyConstants.loginRefreshKey(refreshTokenHash), session.getSessionId(),
                refreshTtlSeconds, TimeUnit.SECONDS);
    }

/**
 * 使指定会话ID失效
 * @param sessionId 要失效的会话ID
 */
    private void invalidateSession(String sessionId) {
        // 检查sessionId是否为空或空白字符串
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        // 从Redis中获取登录会话信息  admin:login:session:{sessionId} -> LoginSession
        LoginSession session = redisOperator.get(RedisKeyConstants.loginSessionKey(sessionId), LoginSession.class);
        // 删除登录会话对应的Redis键值
        redisOperator.delete(RedisKeyConstants.loginSessionKey(sessionId));
        // 如果会话不存在，直接返回
        if (session == null) {
            return;
        }
        // 删除刷新令牌对应的Redis键值  `admin:login:refresh:{refreshTokenHash}` -> `sessionId`
        redisOperator.delete(RedisKeyConstants.loginRefreshKey(session.getRefreshTokenHash()));
        // 获取用户当前保存的会话ID    `admin:login:user:{userId}` -> `sessionId`
        String currentSessionId = redisOperator.get(RedisKeyConstants.loginUserKey(session.getUserId()), String.class);
        // 检查当前会话ID是否与要失效的会话ID一致
        if (sessionId.equals(currentSessionId)) {
            // 如果一致，删除用户登录信息对应的Redis键值
            redisOperator.delete(RedisKeyConstants.loginUserKey(session.getUserId()));
        }
    }

    private boolean isSessionOnline(LoginSession session, String sessionId) {
    /**
     * 生成不透明令牌
     * @return Base64编码的令牌字符串
     */
        if (session == null || !StringUtils.hasText(sessionId) || session.getLoginUser() == null) {
            return false;
        }
        String currentSessionId = redisOperator.get(RedisKeyConstants.loginUserKey(session.getUserId()), String.class);
        return sessionId.equals(currentSessionId);
    }


    private long effectiveRefreshMaxExpireSeconds() {
        return Math.max(refreshExpireSeconds, refreshMaxExpireSeconds);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }


    private String sha256(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    private BusinessException unauthorized(String message) {
        return new BusinessException(CommonConstants.UNAUTHORIZED_CODE, message);
    }
}
