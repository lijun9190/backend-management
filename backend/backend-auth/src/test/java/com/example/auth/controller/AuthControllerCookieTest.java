package com.example.auth.controller;

import com.example.auth.dto.LoginDTO;
import com.example.auth.service.AuthCookieService;
import com.example.auth.service.AuthService;
import com.example.auth.vo.LoginVO;
import com.example.common.constant.CommonConstants;
import com.example.common.model.result.ApiResult;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.Cookie;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证控制器Cookie行为测试，确保令牌只通过HttpOnly Cookie下发。
 */
class AuthControllerCookieTest {

    /**
     * 登录成功后应写入HttpOnly Cookie，并从响应体中移除令牌明文。
     */
    @Test
    void loginShouldSetHttpOnlyCookiesAndHideTokenBodyFields() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService, new AuthCookieService(false));
        LoginDTO dto = new LoginDTO();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authService.login(eq(dto), eq(request))).thenReturn(new LoginVO(
                "access-token",
                "refresh-token",
                CommonConstants.TOKEN_PREFIX.trim(),
                1800L,
                604800L
        ));

        ApiResult<LoginVO> result = controller.login(dto, request, response);

        List<String> cookies = response.getHeaders("Set-Cookie");
        assertTrue(cookies.stream().anyMatch(value -> value.contains(CommonConstants.ACCESS_TOKEN_COOKIE + "=access-token")
                && value.contains("HttpOnly")
                && value.contains("SameSite=Strict")));
        assertTrue(cookies.stream().anyMatch(value -> value.contains(CommonConstants.REFRESH_TOKEN_COOKIE + "=refresh-token")
                && value.contains("HttpOnly")
                && value.contains("SameSite=Strict")));
        assertNull(result.getData().getAccessToken());
        assertNull(result.getData().getRefreshToken());
    }

    /**
     * 刷新接口应从HttpOnly refresh cookie读取令牌，不要求前端提交明文refresh token。
     */
    @Test
    void refreshShouldReadRefreshTokenFromCookie() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService, new AuthCookieService(false));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setCookies(new Cookie(CommonConstants.REFRESH_TOKEN_COOKIE, "refresh-token"));
        when(authService.refreshToken("refresh-token")).thenReturn(new LoginVO(
                "access-token-2",
                "refresh-token-2",
                CommonConstants.TOKEN_PREFIX.trim(),
                1800L,
                604800L
        ));

        ApiResult<LoginVO> result = controller.refresh(null, request, response);

        verify(authService).refreshToken("refresh-token");
        assertNull(result.getData().getAccessToken());
        assertNull(result.getData().getRefreshToken());
    }

    /**
     * 登出接口应能从access cookie识别当前会话，并清理认证Cookie。
     */
    @Test
    void logoutShouldReadAccessTokenFromCookieAndClearCookies() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService, new AuthCookieService(false));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setCookies(new Cookie(CommonConstants.ACCESS_TOKEN_COOKIE, "access-token"));

        controller.logout(null, request, response);

        verify(authService).logout("access-token");
        assertTrue(response.getHeaders("Set-Cookie").stream()
                .anyMatch(value -> value.contains(CommonConstants.ACCESS_TOKEN_COOKIE + "=") && value.contains("Max-Age=0")));
        assertTrue(response.getHeaders("Set-Cookie").stream()
                .anyMatch(value -> value.contains(CommonConstants.REFRESH_TOKEN_COOKIE + "=") && value.contains("Max-Age=0")));
    }
}
