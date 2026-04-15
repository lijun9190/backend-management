package com.example.system.dto.log;

import lombok.Data;

/**
 * 登录日志查询参数。
 */
@Data
public class LoginLogQueryDTO {

    private long current = 1;
    private long size = 10;
    private String username;
    private Integer loginStatus;
}
