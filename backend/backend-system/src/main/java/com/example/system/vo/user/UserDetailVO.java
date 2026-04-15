package com.example.system.vo.user;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户详情。
 */
@Data
public class UserDetailVO {

    private Long id;
    private Long deptId;
    private String username;
    private String nickname;
    private String realName;
    private String phone;
    private String email;
    private Integer gender;
    private Integer status;
    private String avatar;
    private String remark;
    private List<Long> roleIds = new ArrayList<>();
}
