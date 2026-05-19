package com.example.common.security;

import com.example.common.constant.CommonConstants;
import com.example.common.context.LoginUserContext;
import com.example.common.model.security.LoginSession;
import com.example.common.model.security.LoginUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT认证过滤器，负责根据访问令牌恢复当前登录用户上下文。
 */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final LoginSessionManager loginSessionManager;

    /**
     * 创建JWT认证过滤器。
     *
     * @param loginSessionManager 登录会话管理器，用于校验访问令牌并获取登录会话
     */
    public JwtAuthenticationFilter(LoginSessionManager loginSessionManager) {
        this.loginSessionManager = loginSessionManager;
    }

    /**
     * 解析请求中的访问令牌，恢复Spring Security认证信息和线程登录用户上下文。
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet处理异常
     * @throws IOException IO处理异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = resolveToken(request.getHeader("Authorization"));
            if (!StringUtils.hasText(token)) {
                token = resolveTokenFromCookie(request);
            }
            LoginSession session = loginSessionManager.getValidSession(token);
            if (session != null) {
                LoginUser loginUser = session.getLoginUser();
                List<SimpleGrantedAuthority> authorities = loginUser.getPermissions() == null
                        ? Collections.emptyList()
                        : loginUser.getPermissions().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        loginUser, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                LoginUserContext.set(loginUser);
            }
            filterChain.doFilter(request, response);
        } finally {
            LoginUserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 从Authorization请求头中解析访问令牌，兼容带Bearer前缀和裸令牌两种形式。
     *
     * @param authorization Authorization请求头
     * @return 访问令牌；请求头为空时返回null
     */
    private String resolveToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        if (authorization.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return authorization.substring(CommonConstants.TOKEN_PREFIX.length());
        }
        return authorization;
    }

    /**
     * 从HttpOnly Cookie中解析访问令牌，避免前端JavaScript读取令牌明文。
     *
     * @param request HTTP请求
     * @return 访问令牌；不存在时返回null
     */
    private String resolveTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (CommonConstants.ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
