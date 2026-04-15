package com.example.system.dto.role;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 角色新增/编辑参数。
 */
@Data
public class RoleSaveDTO {

    private Long id;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    private Integer status;
    private String remark;
}
