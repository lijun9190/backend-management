package com.example.common.security;

import com.example.common.constant.CommonConstants;
import com.example.common.exception.BusinessException;
import com.example.common.model.security.AuthTokens;
import com.example.common.model.security.LoginSession;
import com.example.common.model.security.LoginUser;
import com.example.common.redis.RedisOperator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class LoginSessionManagerTest {

    @Test
    void shouldKeepOnlyLatestSessionForSingleUser() {
        LoginSessionManager manager = createManager();
        LoginUser loginUser = buildLoginUser(1L, "admin");

        AuthTokens firstTokens = manager.createSession(loginUser);
        AuthTokens secondTokens = manager.createSession(loginUser);

        Assertions.assertNull(manager.getValidSession(firstTokens.getAccessToken()));
        Assertions.assertNotNull(manager.getValidSession(secondTokens.getAccessToken()));

        BusinessException exception = Assertions.assertThrows(
                BusinessException.class,
                () -> manager.refreshSession(firstTokens.getRefreshToken())
        );
        Assertions.assertEquals(CommonConstants.UNAUTHORIZED_CODE, exception.getCode());
    }

    @Test
    void shouldRotateRefreshTokenAndInvalidatePreviousAccessToken() {
        LoginSessionManager manager = createManager();
        LoginUser loginUser = buildLoginUser(2L, "sysadmin");

        AuthTokens firstTokens = manager.createSession(loginUser);
        LoginSession firstSession = manager.getValidSession(firstTokens.getAccessToken());

        AuthTokens refreshedTokens = manager.refreshSession(firstTokens.getRefreshToken());
        LoginSession refreshedSession = manager.getValidSession(refreshedTokens.getAccessToken());

        Assertions.assertNotEquals(firstTokens.getAccessToken(), refreshedTokens.getAccessToken());
        Assertions.assertNotEquals(firstTokens.getRefreshToken(), refreshedTokens.getRefreshToken());
        Assertions.assertNull(manager.getValidSession(firstTokens.getAccessToken()));
        Assertions.assertNotNull(refreshedSession);
        Assertions.assertEquals(firstSession.getSessionId(), refreshedSession.getSessionId());

        BusinessException exception = Assertions.assertThrows(
                BusinessException.class,
                () -> manager.refreshSession(firstTokens.getRefreshToken())
        );
        Assertions.assertEquals(CommonConstants.UNAUTHORIZED_CODE, exception.getCode());
    }

    @Test
    void shouldInvalidateSessionForKickout() {
        LoginSessionManager manager = createManager();
        LoginUser loginUser = buildLoginUser(3L, "operator");

        AuthTokens tokens = manager.createSession(loginUser);
        Assertions.assertNotNull(manager.getValidSession(tokens.getAccessToken()));

        manager.invalidateUserSession(loginUser.getUserId());

        Assertions.assertNull(manager.getValidSession(tokens.getAccessToken()));
        BusinessException exception = Assertions.assertThrows(
                BusinessException.class,
                () -> manager.refreshSession(tokens.getRefreshToken())
        );
        Assertions.assertEquals(CommonConstants.UNAUTHORIZED_CODE, exception.getCode());
    }

    private LoginSessionManager createManager() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret", "unit-test-secret");
        ReflectionTestUtils.setField(provider, "accessExpireSeconds", 600L);

        LoginSessionManager manager = new LoginSessionManager(provider, new InMemoryRedisOperator());
        ReflectionTestUtils.setField(manager, "refreshExpireSeconds", 3600L);
        ReflectionTestUtils.setField(manager, "refreshMaxExpireSeconds", 7200L);
        return manager;
    }

    private LoginUser buildLoginUser(Long userId, String username) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setUsername(username);
        loginUser.setNickname(username);
        return loginUser;
    }

    private static class InMemoryRedisOperator extends RedisOperator {

        private final Map<String, Object> values = new HashMap<>();

        InMemoryRedisOperator() {
            super(null);
        }

        @Override
        public void set(String key, Object value, long timeout, TimeUnit unit) {
            values.put(key, value);
        }

        @Override
        public Object get(String key) {
            return values.get(key);
        }

        @Override
        public boolean hasKey(String key) {
            return values.containsKey(key);
        }

        @Override
        public Boolean delete(String key) {
            return values.remove(key) != null;
        }
    }
}
