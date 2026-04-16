package com.example.auth.controller;

import com.example.auth.dto.LoginDTO;
import com.example.auth.dto.RefreshTokenDTO;
import com.example.auth.service.AuthService;
import com.example.auth.vo.LoginVO;
import com.example.common.model.result.ApiResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResult<LoginVO> login(@Validated @RequestBody LoginDTO dto, HttpServletRequest request) {
        return ApiResult.success(authService.login(dto, request));
    }

    @PostMapping("/refresh")
    public ApiResult<LoginVO> refresh(@Validated @RequestBody RefreshTokenDTO dto) {
        return ApiResult.success(authService.refreshToken(dto));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return ApiResult.success(null);
    }
}
