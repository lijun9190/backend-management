package com.example.common.constant;

public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    public static final String LOGIN_USER_PREFIX = "admin:login:user:";
    public static final String LOGIN_SESSION_PREFIX = "admin:login:session:";
    public static final String LOGIN_REFRESH_PREFIX = "admin:login:refresh:";

    public static String loginUserKey(Long userId) {
        return LOGIN_USER_PREFIX + userId;
    }

    public static String loginSessionKey(String sessionId) {
        return LOGIN_SESSION_PREFIX + sessionId;
    }

    public static String loginRefreshKey(String refreshTokenHash) {
        return LOGIN_REFRESH_PREFIX + refreshTokenHash;
    }
}
