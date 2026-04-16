package com.example.common.model.security;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginSession implements Serializable {

    private String sessionId;
    private Long userId;
    private LoginUser loginUser;
    private String refreshTokenHash;
    private Integer accessTokenVersion;
    private Long refreshExpireAt;
    private Long sessionExpireAt;
    private Long lastRefreshAt;
}
