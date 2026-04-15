package com.example.auth.controller;

import com.example.auth.dto.LoginDTO;
import com.example.auth.service.AuthService;
import com.example.auth.vo.LoginVO;
import com.example.common.model.result.ApiResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 登录登出接口。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

/**
 * 认证控制器的构造函数
 * @param authService 认证服务对象，用于处理用户认证相关的业务逻辑
 */
    public AuthController(AuthService authService) {
    // 将传入的认证服务对象赋值给类的成员变量
        this.authService = authService;
    }

/**
 * 处理用户登录请求的接口方法
 * @param dto 包含用户登录信息的DTO对象，使用@Validated进行参数校验
 * @param request HTTP请求对象，用于获取请求相关信息
 * @return 返回ApiResult封装的结果，包含登录成功后的LoginVO对象
 */
    @PostMapping("/login")
    public ApiResult<LoginVO> login(@Validated @RequestBody LoginDTO dto, HttpServletRequest request) {
        return ApiResult.success(authService.login(dto, request)); // 调用认证服务处理登录逻辑并返回结果
    }

/**
 * 处理用户登出请求的接口方法
 * @param authorization 请求头中的Authorization字段，用于身份验证，非必需参数
 * @return 返回一个成功的ApiResult，不包含数据
 */
    @PostMapping("/logout")
    public ApiResult<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
    // 调用authService的logout方法处理登出逻辑，传入Authorization参数
        authService.logout(authorization);
    // 返回成功响应，不携带数据
        return ApiResult.success(null);
    }
}
