package com.example.system.controller;

import com.example.common.model.result.ApiResult;
import com.example.common.model.result.PageResult;
import com.example.system.dto.log.OperationLogQueryDTO;
import com.example.system.service.OperationLogService;
import com.example.system.vo.log.OperationLogPageVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志接口。
 */
@RestController
@RequestMapping("/api/system/operation-logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @PreAuthorize("@perm.hasPermission('system:operation-log:list')")
    @GetMapping("/page")
    public ApiResult<PageResult<OperationLogPageVO>> page(OperationLogQueryDTO dto) {
        return ApiResult.success(operationLogService.pageQuery(dto));
    }
}
