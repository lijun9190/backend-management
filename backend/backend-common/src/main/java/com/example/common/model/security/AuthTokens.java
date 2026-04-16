package com.example.common.model.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokens {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long accessExpireIn;
    private Long refreshExpireIn;
}
