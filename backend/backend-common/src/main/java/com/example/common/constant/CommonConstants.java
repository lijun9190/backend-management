package com.example.common.constant;

/**
 * 通用常量定义。
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    public static final Integer SUCCESS_CODE = 200;
    public static final Integer UNAUTHORIZED_CODE = 401;
    public static final Integer FORBIDDEN_CODE = 403;
    public static final Integer ERROR_CODE = 500;

    public static final Integer STATUS_ENABLED = 1;
    public static final Integer STATUS_DISABLED = 0;

    public static final Integer DELETED_NO = 0;
    public static final Integer DELETED_YES = 1;

    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String TOKEN_PREFIX = "Bearer ";
}
