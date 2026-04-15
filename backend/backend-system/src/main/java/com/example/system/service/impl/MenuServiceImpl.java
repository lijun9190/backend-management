package com.example.system.service.impl;

import com.example.common.context.LoginUserContext;
import com.example.common.entity.SysMenu;
import com.example.common.exception.BusinessException;
import com.example.common.model.security.LoginUser;
import com.example.system.dto.menu.MenuSaveDTO;
import com.example.system.mapper.SysMenuMapper;
import com.example.system.service.MenuService;
import com.example.system.service.OperationLogService;
import com.example.system.vo.menu.MenuTreeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单管理服务实现。
 */
@Service
public class MenuServiceImpl implements MenuService {

    private final SysMenuMapper sysMenuMapper;
    private final OperationLogService operationLogService;

    public MenuServiceImpl(SysMenuMapper sysMenuMapper, OperationLogService operationLogService) {
        this.sysMenuMapper = sysMenuMapper;
        this.operationLogService = operationLogService;
    }

    @Override
    public List<MenuTreeVO> treeList() {
        List<SysMenu> menus = sysMenuMapper.selectAllMenus();
        return buildTree(menus, 0L);
    }

    @Override
    public MenuTreeVO detail(Long id) {
        SysMenu menu = sysMenuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }
        MenuTreeVO vo = new MenuTreeVO();
        BeanUtils.copyProperties(menu, vo);
        return vo;
    }

    @Override
    public void saveMenu(MenuSaveDTO dto) {
        SysMenu menu = new SysMenu();
        BeanUtils.copyProperties(dto, menu);
        fillAudit(menu, true);
        menu.setDeleted(0);
        sysMenuMapper.insert(menu);
        operationLogService.record("菜单管理", "新增", "/api/system/menus", "POST", 1, null);
    }

    @Override
    public void updateMenu(Long id, MenuSaveDTO dto) {
        SysMenu menu = sysMenuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }
        BeanUtils.copyProperties(dto, menu);
        fillAudit(menu, false);
        sysMenuMapper.updateById(menu);
        operationLogService.record("菜单管理", "编辑", "/api/system/menus/" + id, "PUT", 1, null);
    }

    @Override
    public void removeMenu(Long id) {
        if (sysMenuMapper.countByParentId(id) > 0) {
            throw new BusinessException("当前菜单下存在子节点，无法删除");
        }
        SysMenu menu = sysMenuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }
        menu.setDeleted(1);
        fillAudit(menu, false);
        sysMenuMapper.updateById(menu);
        operationLogService.record("菜单管理", "删除", "/api/system/menus/" + id, "DELETE", 1, null);
    }

    private List<MenuTreeVO> buildTree(List<SysMenu> menus, Long parentId) {
        List<MenuTreeVO> list = new ArrayList<>();
        List<SysMenu> current = menus.stream()
                .filter(item -> (item.getParentId() == null ? 0L : item.getParentId())==(parentId))
                .sorted(Comparator.comparing(SysMenu::getSort, Comparator.nullsLast(Integer::compareTo)).thenComparing(SysMenu::getId))
                .collect(Collectors.toList());
        for (SysMenu menu : current) {
            MenuTreeVO vo = new MenuTreeVO();
            BeanUtils.copyProperties(menu, vo);
            vo.setChildren(buildTree(menus, menu.getId()));
            list.add(vo);
        }
        return list;
    }

    private void fillAudit(SysMenu menu, boolean insert) {
        LoginUser loginUser = LoginUserContext.get();
        String username = loginUser == null ? "system" : loginUser.getUsername();
        if (insert) {
            menu.setCreateBy(username);
            menu.setCreateTime(LocalDateTime.now());
        }
        menu.setUpdateBy(username);
        menu.setUpdateTime(LocalDateTime.now());
    }
}
