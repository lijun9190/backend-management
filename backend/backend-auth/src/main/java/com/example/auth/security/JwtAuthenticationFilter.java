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

/**
 * JWT认证过滤器构造函数
 * @param jwtTokenProvider JWT令牌提供者，用于处理JWT令牌相关操作
 * @param redisOperator Redis操作接口，用于与Redis数据库交互
 */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, RedisOperator redisOperator) {
    // 初始化JWT令牌提供者
        this.jwtTokenProvider = jwtTokenProvider;
    // 初始化Redis操作接口
        this.redisOperator = redisOperator;
    }

/**
 * 内部过滤器方法，用于处理JWT认证和授权逻辑
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
        // 从请求头中解析token
            String token = resolveToken(request.getHeader("Authorization"));
        // 验证token是否存在且有效
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            // 从Redis中获取登录用户信息
                LoginUser loginUser = redisOperator.get(RedisKeyConstants.loginTokenKey(token), LoginUser.class);
                if (loginUser != null) {
                // 将用户权限转换为Spring Security所需的权限格式
                    List<SimpleGrantedAuthority> authorities = loginUser.getPermissions() == null
                            ? Collections.emptyList()
                            : loginUser.getPermissions().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                // 创建认证对象
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            loginUser, null, authorities);
                // 将认证信息设置到安全上下文中
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                // 将用户信息设置到登录用户上下文中
                    LoginUserContext.set(loginUser);
                }
            }
        // 执行下一个过滤器
            filterChain.doFilter(request, response);
        } finally {
        // 清理登录用户上下文
            LoginUserContext.clear();
        // 清理安全上下文
            SecurityContextHolder.clearContext();
        }
    }

/**
 * 解析授权令牌的方法
 * @param authorization 授权字符串，可能包含令牌前缀
 * @return 解析后的令牌字符串，如果授权字符串为空则返回null
 */
    private String resolveToken(String authorization) {
    // 检查授权字符串是否为空或空白
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
    // 检查授权字符串是否包含令牌前缀
        if (authorization.startsWith(CommonConstants.TOKEN_PREFIX)) {
        // 如果包含前缀，则返回去除前缀后的子字符串
            return authorization.substring(CommonConstants.TOKEN_PREFIX.length());
        }
    // 如果不包含前缀，直接返回原始授权字符串
        return authorization;
    }
}
