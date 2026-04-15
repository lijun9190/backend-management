package com.example.system.dto.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 重置密码参数。
 */
@Data
public class UserResetPasswordDTO {

    @NotBlank(message = "新密码不能为空")
    private String password;
}
