package com.example.system.dto.role;

import lombok.Data;

/**
 * 角色分页查询参数。
 */
@Data
public class RoleQueryDTO {

    private long current = 1;
    private long size = 10;
    private String roleName;
    private Integer status;
}
