package com.example.common.constant;

/**
 * Redis Key 统一定义，方便后续扩展单点登录和强制下线。
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    public static final String LOGIN_TOKEN_PREFIX = "admin:login:token:";
    public static final String LOGIN_USER_PREFIX = "admin:login:user:";

    public static String loginTokenKey(String token) {
        return LOGIN_TOKEN_PREFIX + token;
    }

    public static String loginUserKey(Long userId) {
        return LOGIN_USER_PREFIX + userId;
    }
}
