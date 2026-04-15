package com.example.system.dto.user;

import lombok.Data;

import java.util.List;

/**
 * 用户分配角色参数。
 */
@Data
public class UserRoleAssignDTO {

    private List<Long> roleIds;
}
