package com.example.system.dto.user;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 用户状态修改参数。
 */
@Data
public class UserStatusDTO {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
