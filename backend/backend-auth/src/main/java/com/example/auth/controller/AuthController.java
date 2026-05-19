package com.example.auth.controller;

import com.example.auth.dto.LoginDTO;
import com.example.auth.dto.RefreshTokenDTO;
import com.example.auth.service.AuthCookieService;
import com.example.auth.service.AuthService;
import com.example.auth.vo.LoginVO;
import com.example.common.model.result.ApiResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    public AuthController(AuthService authService, AuthCookieService authCookieService) {
        this.authService = authService;
        this.authCookieService = authCookieService;
    }

    /**
     * 用户登录，认证令牌通过HttpOnly Cookie下发，响应体不暴露令牌明文。
     */
    @PostMapping("/login")
    public ApiResult<LoginVO> login(@Validated @RequestBody LoginDTO dto, HttpServletRequest request, HttpServletResponse response) {
        LoginVO loginVO = authService.login(dto, request);
        authCookieService.writeTokenCookies(response, loginVO);
        return ApiResult.success(authCookieService.hideTokenBodyFields(loginVO));
    }

    /**
     * 刷新令牌，优先从HttpOnly Cookie读取refresh token，兼容旧请求体字段。
     */
    @PostMapping("/refresh")
    public ApiResult<LoginVO> refresh(@RequestBody(required = false) RefreshTokenDTO dto,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        String refreshToken = dto == null || dto.getRefreshToken() == null
                ? authCookieService.getRefreshToken(request)
                : dto.getRefreshToken();
        LoginVO loginVO = authService.refreshToken(refreshToken);
        authCookieService.writeTokenCookies(response, loginVO);
        return ApiResult.success(authCookieService.hideTokenBodyFields(loginVO));
    }

    /**
     * 用户登出，支持从Authorization头或HttpOnly access cookie中识别当前会话并清理Cookie。
     */
    @PostMapping("/logout")
    public ApiResult<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        String accessToken = authorization == null ? authCookieService.getAccessToken(request) : authorization;
        authService.logout(accessToken);
        authCookieService.clearTokenCookies(response);
        return ApiResult.success(null);
    }
}
