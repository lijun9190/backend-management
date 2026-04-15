package com.example.system.service;

import com.example.common.model.result.PageResult;
import com.example.system.dto.role.RoleQueryDTO;
import com.example.system.dto.role.RoleSaveDTO;
import com.example.system.vo.role.RoleOptionVO;
import com.example.system.vo.role.RolePageVO;

import java.util.List;

/**
 * 角色管理服务。
 */
public interface RoleService {

    PageResult<RolePageVO> pageQuery(RoleQueryDTO dto);

    void saveRole(RoleSaveDTO dto);

    void updateRole(Long id, RoleSaveDTO dto);

    void changeStatus(Long id, Integer status);

    void assignMenus(Long id, List<Long> menuIds);

    List<RoleOptionVO> options();

    List<Long> getMenuIds(Long roleId);
}
