package com.example.system.controller;

import com.example.common.annotation.OperLog;
import com.example.common.model.result.ApiResult;
import com.example.common.model.result.PageResult;
import com.example.system.dto.user.UserQueryDTO;
import com.example.system.dto.user.UserResetPasswordDTO;
import com.example.system.dto.user.UserRoleAssignDTO;
import com.example.system.dto.user.UserSaveDTO;
import com.example.system.dto.user.UserStatusDTO;
import com.example.system.service.UserService;
import com.example.system.vo.user.UserDetailVO;
import com.example.system.vo.user.UserPageVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口。
 */
@RestController
@RequestMapping("/api/system/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("@perm.hasPermission('system:user:list')")
    @GetMapping("/page")
    public ApiResult<PageResult<UserPageVO>> page(UserQueryDTO dto) {
        return ApiResult.success(userService.pageQuery(dto));
    }

    @PreAuthorize("@perm.hasPermission('system:user:list')")
    @GetMapping("/{id}")
    public ApiResult<UserDetailVO> detail(@PathVariable Long id) {
        return ApiResult.success(userService.detail(id));
    }

    @OperLog(module = "用户管理", type = "新增")
    @PreAuthorize("@perm.hasPermission('system:user:add')")
    @PostMapping
    public ApiResult<Void> save(@Validated @RequestBody UserSaveDTO dto) {
        userService.saveUser(dto);
        return ApiResult.success(null);
    }

    @OperLog(module = "用户管理", type = "编辑")
    @PreAuthorize("@perm.hasPermission('system:user:edit')")
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @Validated @RequestBody UserSaveDTO dto) {
        userService.updateUser(id, dto);
        return ApiResult.success(null);
    }

    @OperLog(module = "用户管理", type = "状态切换")
    @PreAuthorize("@perm.hasPermission('system:user:edit')")
    @PutMapping("/{id}/status")
    public ApiResult<Void> changeStatus(@PathVariable Long id, @Validated @RequestBody UserStatusDTO dto) {
        userService.changeStatus(id, dto.getStatus());
        return ApiResult.success(null);
    }

    @OperLog(module = "用户管理", type = "重置密码")
    @PreAuthorize("@perm.hasPermission('system:user:reset-password')")
    @PutMapping("/{id}/reset-password")
    public ApiResult<Void> resetPassword(@PathVariable Long id, @Validated @RequestBody UserResetPasswordDTO dto) {
        userService.resetPassword(id, dto.getPassword());
        return ApiResult.success(null);
    }

    @OperLog(module = "用户管理", type = "分配角色")
    @PreAuthorize("@perm.hasPermission('system:user:assign-role')")
    @PutMapping("/{id}/roles")
    public ApiResult<Void> assignRoles(@PathVariable Long id, @RequestBody UserRoleAssignDTO dto) {
        userService.assignRoles(id, dto.getRoleIds());
        return ApiResult.success(null);
    }

    @OperLog(module = "用户管理", type = "删除")
    @PreAuthorize("@perm.hasPermission('system:user:delete')")
    @DeleteMapping("/{id}")
    public ApiResult<Void> remove(@PathVariable Long id) {
        userService.removeUser(id);
        return ApiResult.success(null);
    }
}
