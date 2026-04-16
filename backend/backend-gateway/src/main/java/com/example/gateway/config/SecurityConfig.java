package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway 使用自定义全局过滤器做登录态校验，这里关闭默认安全拦截，
 * 避免浏览器请求被默认的 CSRF / Basic Auth 机制提前拒绝。
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)//禁用CSRF保护
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)//禁用HTTP Basic认证
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)//禁用表单登录
                .logout(ServerHttpSecurity.LogoutSpec::disable)//禁用登出功能
                .authorizeExchange(exchange -> exchange.anyExchange().permitAll())//允许所有请求
                .build();
    }
}
