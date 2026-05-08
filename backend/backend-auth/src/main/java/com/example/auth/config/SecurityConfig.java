package com.example.auth.config;

import com.example.auth.security.JwtAuthenticationFilter;
import com.example.common.constant.CommonConstants;
import com.example.common.model.result.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

/**
 * 配置Spring Security的安全过滤器链
 * @param http HttpSecurity对象，用于配置安全规则
 * @return 配置好的SecurityFilterChain实例
 * @throws Exception 可能抛出的异常
 */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    // 禁用CSRF保护，因为是无状态的REST API
        http.csrf().disable()
            // 配置会话管理为无状态模式
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
            // 配置请求授权规则
                .authorizeRequests()
            // 允许任何人访问的端点：登录、刷新令牌和错误页面
                .antMatchers("/api/auth/login", "/api/auth/refresh", "/error").permitAll()
            // 所有其他请求都需要认证
                .anyRequest().authenticated()
                .and()
            // 配置异常处理
                .exceptionHandling()
            // 设置未认证入口点，当用户未登录时返回自定义响应
                .authenticationEntryPoint((request, response, ex) -> writeJson(response,
                        CommonConstants.UNAUTHORIZED_CODE, "未登录或登录状态已过期"))
            // 设置访问拒绝处理器，当用户无权限访问时返回自定义响应
                .accessDeniedHandler((request, response, ex) -> writeJson(response,
                        CommonConstants.FORBIDDEN_CODE, "没有访问该资源的权限"))
                .and()
            // 在用户名密码认证过滤器之前添加JWT认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeJson(HttpServletResponse response, Integer code, String message) throws java.io.IOException {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResult.fail(code, message)));
    }
}
