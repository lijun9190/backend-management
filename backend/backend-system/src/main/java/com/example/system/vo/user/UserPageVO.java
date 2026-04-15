package com.example.system.vo.user;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户分页列表项。
 */
@Data
public class UserPageVO {

    private Long id;
    private Long deptId;
    private String deptName;
    private String username;
    private String nickname;
    private String realName;
    private String phone;
    private String email;
    private Integer gender;
    private Integer status;
    private List<String> roleNames = new ArrayList<>();
    private LocalDateTime createTime;
}
