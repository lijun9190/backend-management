package com.example.common.model.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.example.common.constant.CommonConstants;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 当前登录用户信息模型。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginUser implements Serializable {

    private Long userId;
    private Long deptId;
    private String username;
    private String nickname;
    private String deptName;
    private Integer status;
    private List<String> roles = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();

    @JsonIgnore
    public boolean isSuperAdmin() {
        return roles != null && roles.contains(CommonConstants.ROLE_SUPER_ADMIN);
    }
}
