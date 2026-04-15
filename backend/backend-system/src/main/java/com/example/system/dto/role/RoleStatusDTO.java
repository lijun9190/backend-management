package com.example.system.dto.role;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 角色状态修改参数。
 */
@Data
public class RoleStatusDTO {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
