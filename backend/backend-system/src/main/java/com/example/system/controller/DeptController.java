package com.example.system.controller;

import com.example.common.annotation.OperLog;
import com.example.common.model.result.ApiResult;
import com.example.system.dto.dept.DeptSaveDTO;
import com.example.system.service.DeptService;
import com.example.system.vo.dept.DeptTreeVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理接口。
 */
@RestController
@RequestMapping("/api/system/depts")
public class DeptController {

    private final DeptService deptService;

    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    @PreAuthorize("@perm.hasPermission('system:dept:list')")
    @GetMapping("/tree")
    public ApiResult<List<DeptTreeVO>> tree() {
        return ApiResult.success(deptService.treeList());
    }

    @OperLog(module = "部门管理", type = "新增")
    @PreAuthorize("@perm.hasPermission('system:dept:add')")
    @PostMapping
    public ApiResult<Void> save(@Validated @RequestBody DeptSaveDTO dto) {
        deptService.saveDept(dto);
        return ApiResult.success(null);
    }

    @OperLog(module = "部门管理", type = "编辑")
    @PreAuthorize("@perm.hasPermission('system:dept:edit')")
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @Validated @RequestBody DeptSaveDTO dto) {
        deptService.updateDept(id, dto);
        return ApiResult.success(null);
    }

    @OperLog(module = "部门管理", type = "删除")
    @PreAuthorize("@perm.hasPermission('system:dept:delete')")
    @DeleteMapping("/{id}")
    public ApiResult<Void> remove(@PathVariable Long id) {
        deptService.removeDept(id);
        return ApiResult.success(null);
    }
}
