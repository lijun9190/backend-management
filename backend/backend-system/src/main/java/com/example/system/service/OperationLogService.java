package com.example.system.service;

import com.example.common.model.result.PageResult;
import com.example.system.dto.log.OperationLogQueryDTO;
import com.example.system.vo.log.OperationLogPageVO;

/**
 * 操作日志服务。
 */
public interface OperationLogService {

    PageResult<OperationLogPageVO> pageQuery(OperationLogQueryDTO dto);

    void record(String moduleName, String operationType, String requestUri, String requestMethod,
                Integer status, String errorMessage);
}
