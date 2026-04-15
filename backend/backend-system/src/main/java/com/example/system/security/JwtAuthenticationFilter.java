package com.example.system.security;

import com.example.common.constant.CommonConstants;
import com.example.common.constant.RedisKeyConstants;
import com.example.common.context.LoginUserContext;
import com.example.common.model.security.LoginUser;
import com.example.common.redis.RedisOperator;
import com.example.common.security.JwtTokenProvider;
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

/**
 * system 服务 JWT 认证过滤器。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisOperator redisOperator;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, RedisOperator redisOperator) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisOperator = redisOperator;
    }

/**
 * 内部方法：执行过滤器逻辑
 * 该方法用于处理HTTP请求，验证JWT令牌，并设置安全上下文
 *
 * @param request HTTP请求对象
 * @param response HTTP响应对象
 * @param filterChain 过滤器链
 * @throws ServletException Servlet异常
 * @throws IOException IO异常
 */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
        // 从请求头中解析出JWT令牌
            String token = resolveToken(request.getHeader("Authorization"));
        // 检查令牌是否存在且有效
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            // 从Redis中获取登录用户信息
                LoginUser loginUser = redisOperator.get(RedisKeyConstants.loginTokenKey(token), LoginUser.class);
            // 如果用户信息存在，则进行权限设置
                if (loginUser != null) {
                // 将用户权限转换为Spring Security所需的权限列表
                    List<SimpleGrantedAuthority> authorities = loginUser.getPermissions() == null
                            ? Collections.emptyList()
                            : loginUser.getPermissions().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                // 创建认证令牌并设置到安全上下文中
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            loginUser, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                // 将当前登录用户信息设置到上下文中
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
