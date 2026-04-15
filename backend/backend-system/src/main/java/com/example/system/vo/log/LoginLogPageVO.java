package com.example.system.vo.log;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志分页项。
 */
@Data
public class LoginLogPageVO {

    private Long id;
    private String username;
    private String nickname;
    private String loginIp;
    private String browser;
    private String os;
    private Integer loginStatus;
    private String message;
    private LocalDateTime loginTime;
}
