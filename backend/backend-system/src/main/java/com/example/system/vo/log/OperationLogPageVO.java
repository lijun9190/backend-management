package com.example.system.vo.log;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志分页项。
 */
@Data
public class OperationLogPageVO {

    private Long id;
    private String moduleName;
    private String operationType;
    private String requestMethod;
    private String requestUri;
    private String operatorName;
    private String operatorUsername;
    private Integer operationStatus;
    private Long costTime;
    private String errorMessage;
    private LocalDateTime operationTime;
}
