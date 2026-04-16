package com.example.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RefreshTokenDTO {

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
