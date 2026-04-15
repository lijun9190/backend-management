package com.example.system.controller;

import com.example.common.model.result.ApiResult;
import com.example.common.model.result.PageResult;
import com.example.system.dto.log.LoginLogQueryDTO;
import com.example.system.service.LoginLogService;
import com.example.system.vo.log.LoginLogPageVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录日志接口。
 */
@RestController
@RequestMapping("/api/system/login-logs")
public class LoginLogController {

    private final LoginLogService loginLogService;

    public LoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    @PreAuthorize("@perm.hasPermission('system:login-log:list')")
    @GetMapping("/page")
    public ApiResult<PageResult<LoginLogPageVO>> page(LoginLogQueryDTO dto) {
        return ApiResult.success(loginLogService.pageQuery(dto));
    }
}
