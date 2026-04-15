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

/**
 * 获取当前用户资料的接口方法
 * 使用GET方法请求/profile路径
 *
 * @return ApiResult<UserProfileVO> 返回一个包含用户资料信息的API结果对象
 */
    @GetMapping("/profile")
    public ApiResult<UserProfileVO> profile() {
    // 调用authService的getCurrentUserProfile方法获取当前用户资料
    // 并使用ApiResult.success方法封装成统一的API返回格式
        return ApiResult.success(authService.getCurrentUserProfile());
    }

    @PutMapping("/password")
    public ApiResult<Void> updatePassword(@Validated @RequestBody UpdatePasswordDTO dto) {
        authService.updatePassword(dto);
        return ApiResult.success(null);
    }
}
