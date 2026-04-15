package com.example.gateway.filter;

import com.example.common.constant.CommonConstants;
import com.example.common.constant.RedisKeyConstants;
import com.example.common.model.result.ApiResult;
import com.example.common.security.JwtTokenProvider;
import com.example.common.redis.RedisOperator;
import com.example.gateway.config.IgnoreWhiteProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关全局过滤器。
 *
 * 这里只做基础登录态校验，不做细粒度权限判断。
 */
@Component
@RequiredArgsConstructor
public class AuthTokenGlobalFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisOperator redisOperator;
    private final IgnoreWhiteProperties ignoreWhiteProperties;
    private final ObjectMapper objectMapper;

/**
 * 网关过滤器方法，用于处理请求的认证和授权
 * @param exchange 当前请求交换对象，包含请求和响应信息
 * @param chain 网关过滤器链，用于传递请求到下一个过滤器
 * @return Mono<Void> 表示异步处理的结果
 */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    // 获取当前请求对象
        ServerHttpRequest request = exchange.getRequest();
    // 获取请求路径
        String requestPath = request.getURI().getPath();

    // 如果是OPTIONS请求请求或在忽略白名单中，则直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethodValue()) || ignoreWhiteProperties.match(requestPath)) {
            return chain.filter(exchange);
        }

    // 从请求头中解析token
        String token = resolveToken(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    // 如果token为空，返回未授权响应
        if (!StringUtils.hasText(token)) {
            return writeUnauthorized(exchange.getResponse(), "未检测到登录 token");
        }

    // 验证token的有效性
        if (!jwtTokenProvider.validateToken(token)) {
            return writeUnauthorized(exchange.getResponse(), "登录状态已失效，请重新登录");
        }

    // 检查token是否存在于Redis中（验证是否已登出）
        if (!redisOperator.hasKey(RedisKeyConstants.loginTokenKey(token))) {
            return writeUnauthorized(exchange.getResponse(), "Token 已失效或已退出登录");
        }

    // 构建新的请求头，添加用户信息
        ServerHttpRequest mutateRequest = request.mutate()
                .header("X-User-Id", String.valueOf(jwtTokenProvider.getUserId(token)))
                .header("X-Username", String.valueOf(jwtTokenProvider.getUsername(token)))
                .build();
    // 将修改后的请求传递到下一个过滤器
        return chain.filter(exchange.mutate().request(mutateRequest).build());
    }

/**
 * 解析授权令牌的方法
 * 从授权字符串中提取实际的令牌部分
 * @param authorization 授权字符串，可能包含令牌前缀
 * @return 解析后的令牌字符串，如果输入为空则返回null
 */
    private String resolveToken(String authorization) {
    // 检查授权字符串是否为空或空白
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
    // 检查授权字符串是否包含令牌前缀
        if (authorization.startsWith(CommonConstants.TOKEN_PREFIX)) {
        // 如果包含前缀，则返回去掉前缀后的部分
            return authorization.substring(CommonConstants.TOKEN_PREFIX.length());
        }
    // 如果不包含前缀，直接返回原始字符串
        return authorization;
    }

/**
 * 写入未授权响应信息
 * @param response 服务器响应对象
 * @param message 未授权错误信息
 * @return 返回一个Mono<Void>对象，表示异步操作完成
 */
    private Mono<Void> writeUnauthorized(ServerHttpResponse response, String message) {
    // 设置HTTP响应状态码为401未授权
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
    // 设置响应内容类型为JSON
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    // 设置响应内容编码为UTF-8
        response.getHeaders().set(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name());
    // 将未授权的错误信息转换为字节数组
        byte[] bytes = toBytes(ApiResult.fail(CommonConstants.UNAUTHORIZED_CODE, message));
    // 使用响应缓冲工厂包装字节数组并写入响应体
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    private byte[] toBytes(ApiResult<Void> result) {
        try {
            return objectMapper.writeValueAsBytes(result);
        } catch (JsonProcessingException e) {
            return "{\"code\":401,\"message\":\"未登录\",\"success\":false}".getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
