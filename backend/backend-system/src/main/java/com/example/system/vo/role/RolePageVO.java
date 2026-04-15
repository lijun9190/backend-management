package com.example.system.vo.role;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色分页列表项。
 */
@Data
public class RolePageVO {

    private Long id;
    private String roleName;
    private String roleCode;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
