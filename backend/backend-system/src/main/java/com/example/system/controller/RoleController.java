package com.example.system.controller;

import com.example.common.annotation.OperLog;
import com.example.common.model.result.ApiResult;
import com.example.common.model.result.PageResult;
import com.example.system.dto.role.RoleMenuAssignDTO;
import com.example.system.dto.role.RoleQueryDTO;
import com.example.system.dto.role.RoleSaveDTO;
import com.example.system.dto.role.RoleStatusDTO;
import com.example.system.service.RoleService;
import com.example.system.vo.role.RoleOptionVO;
import com.example.system.vo.role.RolePageVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理接口。
 */
@RestController
@RequestMapping("/api/system/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PreAuthorize("@perm.hasPermission('system:role:list')")
    @GetMapping("/page")
    public ApiResult<PageResult<RolePageVO>> page(RoleQueryDTO dto) {
        return ApiResult.success(roleService.pageQuery(dto));
    }

    @PreAuthorize("@perm.hasPermission('system:role:list')")
    @GetMapping("/options")
    public ApiResult<List<RoleOptionVO>> options() {
        return ApiResult.success(roleService.options());
    }

    @PreAuthorize("@perm.hasPermission('system:role:list')")
    @GetMapping("/{id}/menu-ids")
    public ApiResult<List<Long>> menuIds(@PathVariable Long id) {
        return ApiResult.success(roleService.getMenuIds(id));
    }

    @OperLog(module = "角色管理", type = "新增")
    @PreAuthorize("@perm.hasPermission('system:role:add')")
    @PostMapping
    public ApiResult<Void> save(@Validated @RequestBody RoleSaveDTO dto) {
        roleService.saveRole(dto);
        return ApiResult.success(null);
    }

    @OperLog(module = "角色管理", type = "编辑")
    @PreAuthorize("@perm.hasPermission('system:role:edit')")
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @Validated @RequestBody RoleSaveDTO dto) {
        roleService.updateRole(id, dto);
        return ApiResult.success(null);
    }

    @OperLog(module = "角色管理", type = "状态切换")
    @PreAuthorize("@perm.hasPermission('system:role:edit')")
    @PutMapping("/{id}/status")
    public ApiResult<Void> changeStatus(@PathVariable Long id, @Validated @RequestBody RoleStatusDTO dto) {
        roleService.changeStatus(id, dto.getStatus());
        return ApiResult.success(null);
    }

    @OperLog(module = "角色管理", type = "分配权限")
    @PreAuthorize("@perm.hasPermission('system:role:assign')")
    @PutMapping("/{id}/menus")
    public ApiResult<Void> assignMenus(@PathVariable Long id, @RequestBody RoleMenuAssignDTO dto) {
        roleService.assignMenus(id, dto.getMenuIds());
        return ApiResult.success(null);
    }
}
