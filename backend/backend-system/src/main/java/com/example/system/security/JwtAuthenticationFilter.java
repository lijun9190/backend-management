package com.example.system.security;

import com.example.common.constant.CommonConstants;
import com.example.common.context.LoginUserContext;
import com.example.common.model.security.LoginSession;
import com.example.common.model.security.LoginUser;
import com.example.common.security.LoginSessionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
            String token = resolveToken(request.getHeader("Authorization"));
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
