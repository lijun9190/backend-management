package com.example.system.controller;

import com.example.common.annotation.OperLog;
import com.example.common.model.result.ApiResult;
import com.example.system.dto.menu.MenuSaveDTO;
import com.example.system.service.MenuService;
import com.example.system.vo.menu.MenuTreeVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理接口。
 */
@RestController
@RequestMapping("/api/system/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @PreAuthorize("@perm.hasPermission('system:menu:list')")
    @GetMapping("/tree")
    public ApiResult<List<MenuTreeVO>> tree() {
        return ApiResult.success(menuService.treeList());
    }

    @PreAuthorize("@perm.hasPermission('system:menu:list')")
    @GetMapping("/{id}")
    public ApiResult<MenuTreeVO> detail(@PathVariable Long id) {
        return ApiResult.success(menuService.detail(id));
    }

    @OperLog(module = "菜单管理", type = "新增")
    @PreAuthorize("@perm.hasPermission('system:menu:add')")
    @PostMapping
    public ApiResult<Void> save(@Validated @RequestBody MenuSaveDTO dto) {
        menuService.saveMenu(dto);
        return ApiResult.success(null);
    }

    @OperLog(module = "菜单管理", type = "编辑")
    @PreAuthorize("@perm.hasPermission('system:menu:edit')")
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @Validated @RequestBody MenuSaveDTO dto) {
        menuService.updateMenu(id, dto);
        return ApiResult.success(null);
    }

    @OperLog(module = "菜单管理", type = "删除")
    @PreAuthorize("@perm.hasPermission('system:menu:delete')")
    @DeleteMapping("/{id}")
    public ApiResult<Void> remove(@PathVariable Long id) {
        menuService.removeMenu(id);
        return ApiResult.success(null);
    }
}
