package com.example.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * 白名单配置，统一管理无需登录即可访问的路径。
 */
@Data
@ConfigurationProperties(prefix = "security.ignore")
public class IgnoreWhiteProperties {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * 默认白名单列表，后续可在 yml 中覆盖或追加。
     */
    private List<String> urls = new ArrayList<>();

    public boolean match(String requestPath) {
        if (requestPath == null) {
            return false;
        }
        return urls.stream().anyMatch(item -> antPathMatcher.match(item, requestPath));
    }
}
