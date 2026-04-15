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

/**
 * 查询部门树形结构接口
 * 使用@PreAuthorize注解进行权限控制，需要'system:dept:list'权限才能访问
 *
 * @return 返回部门树形结构数据的ApiResult封装结果
 */
    @PreAuthorize("@perm.hasPermission('system:dept:list')")
    @GetMapping("/tree")
    public ApiResult<List<DeptTreeVO>> tree() {
        return ApiResult.success(deptService.treeList());
    }

/**
 * 操作日志注解：记录模块名称为"部门管理"，操作类型为"新增"
 */
    @OperLog(module = "部门管理", type = "新增")
/**
 * 权限控制注解：检查当前用户是否具有"system:dept:add"权限
 * 只有拥有该权限的用户才能访问此接口
 */
    @PreAuthorize("@perm.hasPermission('system:dept:add')")
/**
 * POST请求映射：处理HTTP POST请求
 * 用于新增部门信息
 */
    @PostMapping
/**
 * 新增部门接口
 * @param dto 部门保存数据传输对象，包含新增部门所需的所有信息
 * @return 返回操作结果，成功时返回成功状态码和null数据
 */
    public ApiResult<Void> save(@Validated @RequestBody DeptSaveDTO dto) {
    // 调用部门服务层的保存方法，将部门信息保存到数据库
        deptService.saveDept(dto);
    // 返回成功结果，不附带具体数据
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
