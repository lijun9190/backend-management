package com.example.system.dto.role;

import lombok.Data;

import java.util.List;

/**
 * 角色分配菜单参数。
 */
@Data
public class RoleMenuAssignDTO {

    private List<Long> menuIds;
}
