package com.example.common.security;

import com.example.common.model.security.LoginUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * JWT 工具测试，先覆盖最关键的生成与解析能力。
 */
class JwtTokenProviderTest {

    @Test
    void shouldCreateAndParseAccessToken() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret", "unit-test-secret");
        ReflectionTestUtils.setField(provider, "accessExpireSeconds", 3600L);

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setUsername("admin");
        loginUser.setNickname("超级管理员");

        String token = provider.createAccessToken(loginUser, "session-1", 3);

        Assertions.assertNotNull(token);
        Assertions.assertTrue(provider.validateAccessToken(token));
        Assertions.assertEquals(Long.valueOf(1L), provider.getUserId(token));
        Assertions.assertEquals("admin", provider.getUsername(token));
        Assertions.assertEquals("session-1", provider.getSessionId(token));
        Assertions.assertEquals(Integer.valueOf(3), provider.getAccessTokenVersion(token));
        Assertions.assertEquals("access", provider.getTokenType(token));
    }
}
