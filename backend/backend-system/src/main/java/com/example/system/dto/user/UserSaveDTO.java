package com.example.system.dto.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 用户新增/编辑参数。
 */
@Data
public class UserSaveDTO {

    private Long id;

    @NotNull(message = "所属部门不能为空")
    private Long deptId;

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String realName;
    private String phone;
    private String email;
    private Integer gender;
    private Integer status;
    private String avatar;
    private String remark;
    private List<Long> roleIds;
}
