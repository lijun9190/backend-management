package com.example.auth.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 当前登录用户信息。
 */
@Data
public class UserProfileVO {

    private Long userId;
    private String username;
    private String nickname;
    private String realName;
    private String deptName;
    private String phone;
    private String email;
    private String avatar;
    private List<String> roles = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();
    private List<UserMenuVO> menus = new ArrayList<>();
}
