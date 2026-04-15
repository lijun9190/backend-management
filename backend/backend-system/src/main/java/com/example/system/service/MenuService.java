package com.example.system.service;

import com.example.system.dto.menu.MenuSaveDTO;
import com.example.system.vo.menu.MenuTreeVO;

import java.util.List;

/**
 * 菜单管理服务。
 */
public interface MenuService {

    List<MenuTreeVO> treeList();

    MenuTreeVO detail(Long id);

    void saveMenu(MenuSaveDTO dto);

    void updateMenu(Long id, MenuSaveDTO dto);

    void removeMenu(Long id);
}
