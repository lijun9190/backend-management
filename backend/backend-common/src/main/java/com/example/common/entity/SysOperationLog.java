package com.example.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体。
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String moduleName;
    private String operationType;
    private String requestMethod;
    private String requestUri;
    private String operatorName;
    private String operatorUsername;
    private String requestParams;
    private Integer operationStatus;
    private Long costTime;
    private String errorMessage;
    private LocalDateTime operationTime;
}
