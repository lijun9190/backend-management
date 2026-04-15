package com.example.common.exception;

import lombok.Getter;

/**
 * 自定义业务异常，统一用于向前端返回友好提示。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
