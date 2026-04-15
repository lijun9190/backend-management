package com.example.common.security;

import com.example.common.context.LoginUserContext;
import com.example.common.model.security.LoginUser;
import org.springframework.stereotype.Component;

/**
 * Spring Security 方法级权限判断 Bean。
 */
@Component("perm")
public class PermissionEvaluatorBean {

    public boolean hasPermission(String permissionCode) {
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            return false;
        }
        if (loginUser.isSuperAdmin()) {
            return true;
        }
        return loginUser.getPermissions() != null && loginUser.getPermissions().contains(permissionCode);
    }
}
