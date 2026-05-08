package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * 跨域配置。
 *
 * 由于前端开发环境和网关端口不同，这里必须统一放开跨域。
 */
@Configuration
public class CorsConfig {

/**
 * 配置跨域资源共享(CORS)的Web过滤器
 * 该方法创建一个CorsWebFilter Bean，用于处理跨域请求
 *
 * @return 配置好的CorsWebFilter实例
 */
    @Bean
    public CorsWebFilter corsWebFilter() {
    // 创建CORS配置对象
        CorsConfiguration config = new CorsConfiguration();
    // 设置允许的源模式，这里配置为允许所有来源
    // 在生产环境中，应该设置为具体的前端域名，如["http://example.com"]
        config.setAllowedOriginPatterns(Collections.singletonList("*"));
    // 设置允许的HTTP方法
        config.setAllowedMethods(Arrays.asList(
                HttpMethod.GET.name(),      // 允许GET方法
                HttpMethod.POST.name(),     // 允许POST方法
                HttpMethod.PUT.name(),      // 允许PUT方法
                HttpMethod.DELETE.name(),   // 允许DELETE方法
                HttpMethod.OPTIONS.name(),  // 允许OPTIONS方法
                HttpMethod.PATCH.name()     // 允许PATCH方法
        ));
    // 设置允许的请求头，这里配置为允许所有请求头
    // 在生产环境中，应该设置为具体的请求头列表
        config.setAllowedHeaders(Collections.singletonList("*"));
    // 是否允许发送Cookie信息
        config.setAllowCredentials(true);
    // 预检请求的有效期，单位为秒
        config.setMaxAge(3600L);

    // 创建基于URL的CORS配置源
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    // 注册CORS配置，对所有的URL路径(**/)应用此配置
        source.registerCorsConfiguration("/**", config);
    // 创建并返回CorsWebFilter实例
        return new CorsWebFilter(source);
    }
}
