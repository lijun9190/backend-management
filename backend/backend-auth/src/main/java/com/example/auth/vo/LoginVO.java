package com.example.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    private String accessToken;
    private String tokenType;
    private Long expireIn;
}
