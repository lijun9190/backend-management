package com.example.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.constant.CommonConstants;
import com.example.common.context.LoginUserContext;
import com.example.common.entity.SysDept;
import com.example.common.entity.SysUser;
import com.example.common.entity.SysUserRole;
import com.example.common.exception.BusinessException;
import com.example.common.model.result.PageResult;
import com.example.common.model.security.LoginUser;
import com.example.common.security.LoginSessionManager;
import com.example.system.dto.user.UserQueryDTO;
import com.example.system.dto.user.UserSaveDTO;
import com.example.system.mapper.SysDeptMapper;
import com.example.system.mapper.SysRoleMapper;
import com.example.system.mapper.SysUserMapper;
import com.example.system.mapper.SysUserRoleMapper;
import com.example.system.service.OperationLogService;
import com.example.system.service.UserService;
import com.example.system.vo.user.UserDetailVO;
import com.example.system.vo.user.UserPageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;
    private final LoginSessionManager loginSessionManager;

    public UserServiceImpl(SysUserMapper sysUserMapper,
                           SysDeptMapper sysDeptMapper,
                           SysRoleMapper sysRoleMapper,
                           SysUserRoleMapper sysUserRoleMapper,
                           PasswordEncoder passwordEncoder,
                           OperationLogService operationLogService,
                           LoginSessionManager loginSessionManager) {
        this.sysUserMapper = sysUserMapper;
        this.sysDeptMapper = sysDeptMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.operationLogService = operationLogService;
        this.loginSessionManager = loginSessionManager;
    }

    @Override
    public PageResult<UserPageVO> pageQuery(UserQueryDTO dto) {
        Page<SysUser> page = new Page<>(dto.getCurrent(), dto.getSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getDeleted, CommonConstants.DELETED_NO)
                .like(StringUtils.hasText(dto.getUsername()), SysUser::getUsername, dto.getUsername())
                .like(StringUtils.hasText(dto.getNickname()), SysUser::getNickname, dto.getNickname())
                .eq(dto.getStatus() != null, SysUser::getStatus, dto.getStatus())
                .orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = sysUserMapper.selectPage(page, wrapper);

        PageResult<UserPageVO> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords().stream().map(user -> {
            UserPageVO vo = new UserPageVO();
            BeanUtils.copyProperties(user, vo);
            SysDept dept = user.getDeptId() == null ? null : sysDeptMapper.selectById(user.getDeptId());
            vo.setDeptName(dept == null ? null : dept.getDeptName());
            vo.setRoleNames(sysUserMapper.selectRoleNamesByUserId(user.getId()));
            return vo;
        }).collect(Collectors.toList()));
        pageResult.setCurrent(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    @Override
    public UserDetailVO detail(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null || CommonConstants.DELETED_YES.equals(user.getDeleted())) {
            throw new BusinessException("用户不存在");
        }
        UserDetailVO vo = new UserDetailVO();
        BeanUtils.copyProperties(user, vo);
        List<Long> roleIds = sysRoleMapper.selectRoleIdsByUserId(id);
        vo.setRoleIds(roleIds == null ? Collections.emptyList() : roleIds);
        return vo;
    }

    @Override
    public void saveUser(UserSaveDTO dto) {
        if (sysUserMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(passwordEncoder.encode(StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : "Admin@123456"));
        user.setDeleted(CommonConstants.DELETED_NO);
        fillAudit(user, true);
        sysUserMapper.insert(user);
        assignRoles(user.getId(), dto.getRoleIds());
        operationLogService.record("用户管理", "新增", "/api/system/users", "POST", 1, null);
    }

    @Override
    public void updateUser(Long id, UserSaveDTO dto) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null || CommonConstants.DELETED_YES.equals(user.getDeleted())) {
            throw new BusinessException("用户不存在");
        }
        BeanUtils.copyProperties(dto, user, "password", "username");
        fillAudit(user, false);
        sysUserMapper.updateById(user);
        assignRoles(id, dto.getRoleIds());
        operationLogService.record("用户管理", "编辑", "/api/system/users/" + id, "PUT", 1, null);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null || CommonConstants.DELETED_YES.equals(user.getDeleted())) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        fillAudit(user, false);
        sysUserMapper.updateById(user);
        // 用户状态变化会影响登录资格，立即清理会话避免禁用账号继续访问。
        loginSessionManager.invalidateUserSession(id);
        operationLogService.record("用户管理", "状态切换", "/api/system/users/" + id + "/status", "PUT", 1, null);
    }

    @Override
    public void resetPassword(Long id, String password) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null || CommonConstants.DELETED_YES.equals(user.getDeleted())) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(password));
        fillAudit(user, false);
        sysUserMapper.updateById(user);
        // 密码重置后旧会话不再可信，强制用户使用新密码重新登录。
        loginSessionManager.invalidateUserSession(id);
        operationLogService.record("用户管理", "重置密码", "/api/system/users/" + id + "/reset-password", "PUT", 1, null);
    }

    @Override
    public void assignRoles(Long id, List<Long> roleIds) {
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(id);
                userRole.setRoleId(roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }
        // 角色变更会改变权限集合，清理旧会话避免继续使用缓存权限。
        loginSessionManager.invalidateUserSession(id);
        operationLogService.record("用户管理", "分配角色", "/api/system/users/" + id + "/roles", "PUT", 1, null);
    }

    @Override
    public void kickout(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null || CommonConstants.DELETED_YES.equals(user.getDeleted())) {
            throw new BusinessException("用户不存在");
        }
        LoginUser currentUser = LoginUserContext.get();
        if (currentUser != null && id.equals(currentUser.getUserId())) {
            throw new BusinessException("当前登录用户不允许踢自己下线");
        }
        loginSessionManager.invalidateUserSession(id);
        operationLogService.record("用户管理", "强制下线", "/api/system/users/" + id + "/session", "DELETE", 1, null);
    }

    @Override
    public void removeUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null || CommonConstants.DELETED_YES.equals(user.getDeleted())) {
            throw new BusinessException("用户不存在");
        }
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser != null && id.equals(loginUser.getUserId())) {
            throw new BusinessException("当前登录用户不允许删除");
        }
        List<String> roleCodes = sysRoleMapper.selectRoleCodesByUserId(id);
        if (roleCodes != null && roleCodes.contains(CommonConstants.ROLE_SUPER_ADMIN)) {
            throw new BusinessException("超级管理员不允许删除");
        }

        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        loginSessionManager.invalidateUserSession(id);
        user.setDeleted(CommonConstants.DELETED_YES);
        fillAudit(user, false);
        sysUserMapper.updateById(user);
        operationLogService.record("用户管理", "删除", "/api/system/users/" + id, "DELETE", 1, null);
    }

    private void fillAudit(SysUser user, boolean insert) {
        LoginUser loginUser = LoginUserContext.get();
        String username = loginUser == null ? "system" : loginUser.getUsername();
        if (insert) {
            user.setCreateBy(username);
            user.setCreateTime(LocalDateTime.now());
        }
        user.setUpdateBy(username);
        user.setUpdateTime(LocalDateTime.now());
    }
}
