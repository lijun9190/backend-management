package com.example.common.enums;

/**
 * 启用状态枚举。
 */
public enum StatusEnum {
    DISABLED(0),
    ENABLED(1);

    private final int value;

    StatusEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
