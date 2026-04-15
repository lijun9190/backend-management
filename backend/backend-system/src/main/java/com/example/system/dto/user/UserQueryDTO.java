package com.example.system.dto.user;

import lombok.Data;

/**
 * 用户分页查询参数。
 */
@Data
public class UserQueryDTO {

    private long current = 1;
    private long size = 10;
    private String username;
    private String nickname;
    private Integer status;
}
