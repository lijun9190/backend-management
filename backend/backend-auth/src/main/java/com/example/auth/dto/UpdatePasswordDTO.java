package com.example.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 修改个人密码参数。
 */
@Data
public class UpdatePasswordDTO {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
