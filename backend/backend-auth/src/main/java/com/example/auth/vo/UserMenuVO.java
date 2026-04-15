package com.example.auth.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 当前用户菜单树节点。
 */
@Data
public class UserMenuVO {

    private Long id;
    private Long parentId;
    private String name;
    private String menuType;
    private String path;
    private String component;
    private String routeName;
    private String icon;
    private Integer sort;
    private String permissionCode;
    private Integer visible;
    private Integer status;
    private List<UserMenuVO> children = new ArrayList<>();
}
