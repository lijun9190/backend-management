package com.example.system.vo.menu;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点。
 */
@Data
public class MenuTreeVO {

    private Long id;
    private Long parentId;
    private String menuName;
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
    private List<MenuTreeVO> children = new ArrayList<>();
}
