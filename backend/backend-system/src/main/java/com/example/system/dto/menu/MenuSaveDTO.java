package com.example.system.dto.menu;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 菜单新增/编辑参数。
 */
@Data
public class MenuSaveDTO {

    private Long id;

    @NotNull(message = "上级菜单不能为空")
    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    private String path;
    private String component;
    private String routeName;
    private String icon;
    private Integer sort;
    private String permissionCode;
    private Integer visible;
    private Integer status;
    private String remark;
}
