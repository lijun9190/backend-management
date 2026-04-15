package com.example.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.context.LoginUserContext;
import com.example.common.entity.SysRole;
import com.example.common.entity.SysRoleMenu;
import com.example.common.exception.BusinessException;
import com.example.common.model.result.PageResult;
import com.example.common.model.security.LoginUser;
import com.example.system.dto.role.RoleQueryDTO;
import com.example.system.dto.role.RoleSaveDTO;
import com.example.system.mapper.SysMenuMapper;
import com.example.system.mapper.SysRoleMapper;
import com.example.system.mapper.SysRoleMenuMapper;
import com.example.system.service.OperationLogService;
import com.example.system.service.RoleService;
import com.example.system.vo.role.RoleOptionVO;
import com.example.system.vo.role.RolePageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理服务实现。
 */
@Service
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysMenuMapper sysMenuMapper;
    private final OperationLogService operationLogService;

    public RoleServiceImpl(SysRoleMapper sysRoleMapper,
                           SysRoleMenuMapper sysRoleMenuMapper,
                           SysMenuMapper sysMenuMapper,
                           OperationLogService operationLogService) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysRoleMenuMapper = sysRoleMenuMapper;
        this.sysMenuMapper = sysMenuMapper;
        this.operationLogService = operationLogService;
    }

    @Override
    public PageResult<RolePageVO> pageQuery(RoleQueryDTO dto) {
        Page<SysRole> page = new Page<>(dto.getCurrent(), dto.getSize());
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getDeleted, 0)
                .like(StringUtils.hasText(dto.getRoleName()), SysRole::getRoleName, dto.getRoleName())
                .eq(dto.getStatus() != null, SysRole::getStatus, dto.getStatus())
                .orderByDesc(SysRole::getCreateTime);
        Page<SysRole> result = sysRoleMapper.selectPage(page, wrapper);
        PageResult<RolePageVO> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords().stream().map(item -> {
            RolePageVO vo = new RolePageVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).collect(Collectors.toList()));
        pageResult.setCurrent(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    @Override
    public void saveRole(RoleSaveDTO dto) {
        SysRole role = new SysRole();
        BeanUtils.copyProperties(dto, role);
        fillAudit(role, true);
        role.setDeleted(0);
        sysRoleMapper.insert(role);
        operationLogService.record("角色管理", "新增", "/api/system/roles", "POST", 1, null);
    }

    @Override
    public void updateRole(Long id, RoleSaveDTO dto) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        BeanUtils.copyProperties(dto, role);
        fillAudit(role, false);
        sysRoleMapper.updateById(role);
        operationLogService.record("角色管理", "编辑", "/api/system/roles/" + id, "PUT", 1, null);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        role.setStatus(status);
        fillAudit(role, false);
        sysRoleMapper.updateById(role);
        operationLogService.record("角色管理", "状态切换", "/api/system/roles/" + id + "/status", "PUT", 1, null);
    }

    @Override
    public void assignMenus(Long id, List<Long> menuIds) {
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                SysRoleMenu roleMenu = new SysRoleMenu();
                roleMenu.setRoleId(id);
                roleMenu.setMenuId(menuId);
                sysRoleMenuMapper.insert(roleMenu);
            }
        }
        operationLogService.record("角色管理", "分配权限", "/api/system/roles/" + id + "/menus", "PUT", 1, null);
    }

    @Override
    public List<RoleOptionVO> options() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getDeleted, 0)
                        .eq(SysRole::getStatus, 1)
                        .orderByAsc(SysRole::getId))
                .stream()
                .map(item -> new RoleOptionVO(item.getId(), item.getRoleName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getMenuIds(Long roleId) {
        return sysMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    private void fillAudit(SysRole role, boolean insert) {
        LoginUser loginUser = LoginUserContext.get();
        String username = loginUser == null ? "system" : loginUser.getUsername();
        if (insert) {
            role.setCreateBy(username);
            role.setCreateTime(LocalDateTime.now());
        }
        role.setUpdateBy(username);
        role.setUpdateTime(LocalDateTime.now());
    }
}
