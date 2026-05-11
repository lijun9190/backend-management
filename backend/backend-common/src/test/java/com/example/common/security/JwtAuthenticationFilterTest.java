package com.example.common.security;

import com.example.common.constant.CommonConstants;
import com.example.common.context.LoginUserContext;
import com.example.common.model.security.LoginSession;
import com.example.common.model.security.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JWT认证过滤器测试，验证认证上下文恢复与清理逻辑。
 */
class JwtAuthenticationFilterTest {

    /**
     * 每个测试结束后清理线程上下文，避免影响后续用例。
     */
    @AfterEach
    void clearContext() {
        LoginUserContext.clear();
        SecurityContextHolder.clearContext();
    }

    /**
     * 验证缺少访问令牌时不会写入认证信息，并且请求会继续进入过滤器链。
     */
    @Test
    void shouldContinueWithoutAuthenticationWhenTokenIsMissing() throws Exception {
        TestLoginSessionManager loginSessionManager = new TestLoginSessionManager(null);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(loginSessionManager);
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
            chainCalled.set(true);
            Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
            Assertions.assertNull(LoginUserContext.get());
        });

        Assertions.assertTrue(chainCalled.get());
        Assertions.assertNull(loginSessionManager.requestToken);
        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
        Assertions.assertNull(LoginUserContext.get());
    }

    /**
     * 验证有效访问令牌会恢复认证信息，并在过滤器链结束后清理上下文。
     */
    @Test
    void shouldSetAuthenticationAndClearContextAfterFilterChain() throws Exception {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setUsername("admin");
        loginUser.setPermissions(Collections.singletonList("system:user:list"));

        LoginSession session = new LoginSession();
        session.setLoginUser(loginUser);
        TestLoginSessionManager loginSessionManager = new TestLoginSessionManager(session);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(loginSessionManager);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", CommonConstants.TOKEN_PREFIX + "access-token");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, new MockHttpServletResponse(), (ServletRequest servletRequest, ServletResponse servletResponse) -> {
            chainCalled.set(true);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Assertions.assertNotNull(authentication);
            Assertions.assertSame(loginUser, authentication.getPrincipal());
            Assertions.assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(authority -> "system:user:list".equals(authority.getAuthority())));
            Assertions.assertSame(loginUser, LoginUserContext.get());
        });

        Assertions.assertTrue(chainCalled.get());
        Assertions.assertEquals("access-token", loginSessionManager.requestToken);
        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
        Assertions.assertNull(LoginUserContext.get());
    }

    /**
     * 测试用登录会话管理器，用于控制过滤器获取到的会话结果。
     */
    private static class TestLoginSessionManager extends LoginSessionManager {

        private final LoginSession session;
        private String requestToken;

        /**
         * 创建固定返回会话的测试管理器。
         *
         * @param session 过滤器校验令牌时返回的登录会话
         */
        TestLoginSessionManager(LoginSession session) {
            super(null, null);
            this.session = session;
        }

        /**
         * 记录过滤器传入的访问令牌，并返回预置登录会话。
         *
         * @param accessToken 访问令牌
         * @return 预置登录会话
         */
        @Override
        public LoginSession getValidSession(String accessToken) {
            this.requestToken = accessToken;
            return session;
        }
    }
}
