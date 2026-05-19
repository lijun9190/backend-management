package com.example.system.service;

import com.example.common.entity.SysMenu;
import com.example.system.mapper.SysMenuMapper;
import com.example.system.service.impl.MenuServiceImpl;
import com.example.system.service.OperationLogService;
import com.example.system.vo.menu.MenuTreeVO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 菜单树构建测试，覆盖Long对象ID比较等边界场景。
 */
class MenuServiceTreeTest {

    /**
     * 验证父子菜单ID超过Long缓存范围时仍能正确挂载子节点。
     */
    @Test
    void treeListShouldLinkChildrenWhenParentIdIsLargeLongValue() {
        SysMenuMapper sysMenuMapper = mock(SysMenuMapper.class);
        OperationLogService operationLogService = mock(OperationLogService.class);
        MenuService menuService = new MenuServiceImpl(sysMenuMapper, operationLogService);
        SysMenu parent = menu(Long.valueOf("200"), 0L, "系统管理");
        SysMenu child = menu(Long.valueOf("201"), Long.valueOf("200"), "用户管理");
        when(sysMenuMapper.selectAllMenus()).thenReturn(Arrays.asList(parent, child));

        List<MenuTreeVO> tree = menuService.treeList();

        assertEquals(1, tree.size());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals(Long.valueOf(201L), tree.get(0).getChildren().get(0).getId());
    }

    /**
     * 构造菜单实体，减少测试中与树构建无关的字段噪音。
     */
    private SysMenu menu(Long id, Long parentId, String menuName) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setMenuName(menuName);
        menu.setMenuType("MENU");
        menu.setSort(1);
        menu.setDeleted(0);
        return menu;
    }
}
