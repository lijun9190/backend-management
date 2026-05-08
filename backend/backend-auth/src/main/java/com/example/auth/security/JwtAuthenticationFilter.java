package com.example.auth.security;

import com.example.common.constant.CommonConstants;
import com.example.common.context.LoginUserContext;
import com.example.common.model.security.LoginSession;
import com.example.common.model.security.LoginUser;
import com.example.common.security.LoginSessionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final LoginSessionManager loginSessionManager;

    public JwtAuthenticationFilter(LoginSessionManager loginSessionManager) {
        this.loginSessionManager = loginSessionManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 从请求头中获取Authorization字段，解析出token
            String token = resolveToken(request.getHeader("Authorization"));
            // 根据token获取有效的登录会话
            LoginSession session = loginSessionManager.getValidSession(token);
            if (session != null) {
                // 从会话中获取登录用户信息
                LoginUser loginUser = session.getLoginUser();
                // 将用户权限转换为Spring Security所需的授权列表
                List<SimpleGrantedAuthority> authorities = loginUser.getPermissions() == null
                        ? Collections.emptyList()  // 如果权限为空，则返回空列表
                        : loginUser.getPermissions().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                // 创建认证令牌，包含用户信息、凭证(设为null)和权限列表
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        loginUser, null, authorities);
                // 将认证信息设置到安全上下文中
                SecurityContextHolder.getContext().setAuthentication(authentication);
                // 将登录用户信息设置到用户上下文中
                LoginUserContext.set(loginUser);
            }
            // 执行过滤器链
            filterChain.doFilter(request, response);
        } finally {
            // 清理用户上下文
            LoginUserContext.clear();
            // 清理安全上下文
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        if (authorization.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return authorization.substring(CommonConstants.TOKEN_PREFIX.length());
        }
        return authorization;
    }
}
