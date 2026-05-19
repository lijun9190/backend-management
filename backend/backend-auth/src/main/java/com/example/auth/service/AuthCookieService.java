package com.example.auth.service;

import com.example.auth.vo.LoginVO;
import com.example.common.constant.CommonConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;

/**
 * 认证Cookie服务，负责下发、读取和清理HttpOnly令牌Cookie。
 */
@Service
public class AuthCookieService {

    private final boolean secureCookie;

    /**
     * 创建认证Cookie服务。
     *
     * @param secureCookie 是否为Cookie添加Secure标记，生产HTTPS环境应开启
     */
    public AuthCookieService(@Value("${security.cookie.secure:false}") boolean secureCookie) {
        this.secureCookie = secureCookie;
    }

    /**
     * 将登录令牌写入HttpOnly Cookie，并避免前端JavaScript读取令牌明文。
     *
     * @param response HTTP响应
     * @param loginVO  登录返回数据
     */
    public void writeTokenCookies(HttpServletResponse response, LoginVO loginVO) {
        addCookie(response, CommonConstants.ACCESS_TOKEN_COOKIE, loginVO.getAccessToken(), loginVO.getAccessExpireIn());
        addCookie(response, CommonConstants.REFRESH_TOKEN_COOKIE, loginVO.getRefreshToken(), loginVO.getRefreshExpireIn());
    }

    /**
     * 清理认证Cookie，用于登出或认证状态失效后覆盖浏览器中的旧令牌。
     *
     * @param response HTTP响应
     */
    public void clearTokenCookies(HttpServletResponse response) {
        addCookie(response, CommonConstants.ACCESS_TOKEN_COOKIE, "", 0L);
        addCookie(response, CommonConstants.REFRESH_TOKEN_COOKIE, "", 0L);
    }

    /**
     * 从请求Cookie中读取访问令牌。
     *
     * @param request HTTP请求
     * @return access token；不存在时返回null
     */
    public String getAccessToken(HttpServletRequest request) {
        return getCookieValue(request, CommonConstants.ACCESS_TOKEN_COOKIE);
    }

    /**
     * 从请求Cookie中读取刷新令牌。
     *
     * @param request HTTP请求
     * @return refresh token；不存在时返回null
     */
    public String getRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, CommonConstants.REFRESH_TOKEN_COOKIE);
    }

    /**
     * 移除响应体中的令牌字段，避免前端通过JSON读取令牌明文。
     *
     * @param loginVO 登录响应对象
     * @return 已清理令牌字段的登录响应对象
     */
    public LoginVO hideTokenBodyFields(LoginVO loginVO) {
        loginVO.setAccessToken(null);
        loginVO.setRefreshToken(null);
        return loginVO;
    }

    /**
     * 添加带安全属性的Set-Cookie响应头。
     *
     * @param response HTTP响应
     * @param name     Cookie名称
     * @param value    Cookie值
     * @param maxAge   最大存活秒数
     */
    private void addCookie(HttpServletResponse response, String name, String value, Long maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value == null ? "" : value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofSeconds(maxAge == null ? 0L : maxAge))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 按名称从请求Cookie数组中查找值。
     *
     * @param request HTTP请求
     * @param name    Cookie名称
     * @return Cookie值；不存在时返回null
     */
    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
