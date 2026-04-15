package com.example.system.vo.role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色下拉选项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleOptionVO {

    private Long id;
    private String roleName;
}
