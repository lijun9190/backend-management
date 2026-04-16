package com.example.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long accessExpireIn;
    private Long refreshExpireIn;
}
