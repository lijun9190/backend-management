package com.example.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解，用于在关键接口上自动记录日志。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    String module();

    String type();
}
