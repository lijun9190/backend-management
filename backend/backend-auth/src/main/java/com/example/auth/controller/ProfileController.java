package com.example.auth.controller;

import com.example.auth.dto.UpdatePasswordDTO;
import com.example.auth.service.AuthService;
import com.example.auth.vo.UserProfileVO;
import com.example.common.model.result.ApiResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录人相关接口。
 */
@RestController
@RequestMapping("/api/auth/user")
public class ProfileController {

    private final AuthService authService;

    public ProfileController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/profile")
    public ApiResult<UserProfileVO> profile() {
        return ApiResult.success(authService.getCurrentUserProfile());
    }

    @PutMapping("/password")
    public ApiResult<Void> updatePassword(@Validated @RequestBody UpdatePasswordDTO dto) {
        authService.updatePassword(dto);
        return ApiResult.success(null);
    }
}
