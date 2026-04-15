package com.example.system.dto.log;

import lombok.Data;

/**
 * 操作日志查询参数。
 */
@Data
public class OperationLogQueryDTO {

    private long current = 1;
    private long size = 10;
    private String moduleName;
    private String operatorName;
}
