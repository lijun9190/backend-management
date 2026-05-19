package com.example.gateway.filter;

import com.example.common.constant.CommonConstants;
import com.example.common.model.result.ApiResult;
import com.example.common.model.security.LoginSession;
import com.example.common.security.LoginSessionManager;
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
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class AuthTokenGlobalFilter implements GlobalFilter, Ordered {

    private final LoginSessionManager loginSessionManager;
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
        if (!StringUtils.hasText(token)) {
            return writeUnauthorized(exchange.getResponse(), "未检测到登录 token");
        }

        // 从登录会话管理器中获取有效的登录会话
        LoginSession session = loginSessionManager.getValidSession(token);
        // 检查会话和登录用户是否存在
        if (session == null || session.getLoginUser() == null) {
            return writeUnauthorized(exchange.getResponse(), "登录状态已失效，请重新登录");
        }

        // 构建新的请求对象，添加用户信息到请求头
        ServerHttpRequest mutateRequest = request.mutate()
                .header("X-User-Id", String.valueOf(session.getLoginUser().getUserId()))
                .header("X-Username", String.valueOf(session.getLoginUser().getUsername()))
                .build();
        // 将新请求传递给下一个过滤器
        return chain.filter(exchange.mutate().request(mutateRequest).build());
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

    private Mono<Void> writeUnauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(new MediaType(MediaType.APPLICATION_JSON, Collections.singletonMap("charset", StandardCharsets.UTF_8.name())));
        byte[] bytes = toBytes(ApiResult.fail(CommonConstants.UNAUTHORIZED_CODE, message));
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
