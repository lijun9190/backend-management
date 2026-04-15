package com.example.auth.security;

import com.example.common.constant.CommonConstants;
import com.example.common.constant.RedisKeyConstants;
import com.example.common.context.LoginUserContext;
import com.example.common.model.security.LoginUser;
import com.example.common.redis.RedisOperator;
import com.example.common.security.JwtTokenProvider;
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

/**
 * JWT 认证过滤器。
 *
 * 这里负责把 token 解析成当前线程可用的登录用户上下文。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisOperator redisOperator;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, RedisOperator redisOperator) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisOperator = redisOperator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = resolveToken(request.getHeader("Authorization"));
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                LoginUser loginUser = redisOperator.get(RedisKeyConstants.loginTokenKey(token), LoginUser.class);
                if (loginUser != null) {
                    List<SimpleGrantedAuthority> authorities = loginUser.getPermissions() == null
                            ? Collections.emptyList()
                            : loginUser.getPermissions().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            loginUser, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    LoginUserContext.set(loginUser);
                }
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
