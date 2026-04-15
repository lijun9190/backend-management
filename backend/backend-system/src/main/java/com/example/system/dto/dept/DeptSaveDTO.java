package com.example.system.dto.dept;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 部门新增/编辑参数。
 */
@Data
public class DeptSaveDTO {

    private Long id;

    @NotNull(message = "上级部门不能为空")
    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    private String leader;
    private String phone;
    private Integer sort;
    private Integer status;
    private String remark;
}
