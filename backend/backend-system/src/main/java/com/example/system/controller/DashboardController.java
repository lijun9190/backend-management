package com.example.system.controller;

import com.example.common.model.result.ApiResult;
import com.example.system.service.DashboardService;
import com.example.system.vo.dashboard.DashboardOverviewVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘接口。
 */
@RestController
@RequestMapping("/api/system/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PreAuthorize("@perm.hasPermission('dashboard:view')")
    @GetMapping("/overview")
    public ApiResult<DashboardOverviewVO> overview() {
        return ApiResult.success(dashboardService.overview());
    }
}
