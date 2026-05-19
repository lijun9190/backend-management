package com.example.system.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.common.context.LoginUserContext;
import com.example.common.entity.SysUser;
import com.example.common.entity.SysUserRole;
import com.example.common.exception.BusinessException;
import com.example.common.model.security.LoginUser;
import com.example.common.security.LoginSessionManager;
import com.example.system.mapper.SysDeptMapper;
import com.example.system.mapper.SysRoleMapper;
import com.example.system.mapper.SysUserMapper;
import com.example.system.mapper.SysUserRoleMapper;
import com.example.system.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户删除与会话失效测试，使用Mock隔离数据库和Redis依赖。
 */
class UserServiceDeleteTest {

    private UserService userService;
    private SysUserMapper sysUserMapper;
    private SysRoleMapper sysRoleMapper;
    private SysUserRoleMapper sysUserRoleMapper;
    private PasswordEncoder passwordEncoder;
    private LoginSessionManager loginSessionManager;

    /**
     * 初始化用户服务及其依赖，保证每个测试之间没有共享Mock状态。
     */
    @BeforeEach
    void setUp() {
        sysUserMapper = mock(SysUserMapper.class);
        SysDeptMapper sysDeptMapper = mock(SysDeptMapper.class);
        sysRoleMapper = mock(SysRoleMapper.class);
        sysUserRoleMapper = mock(SysUserRoleMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        OperationLogService operationLogService = mock(OperationLogService.class);
        loginSessionManager = mock(LoginSessionManager.class);
        userService = new UserServiceImpl(
                sysUserMapper,
                sysDeptMapper,
                sysRoleMapper,
                sysUserRoleMapper,
                passwordEncoder,
                operationLogService,
                loginSessionManager
        );
    }

    /**
     * 清理线程登录上下文，避免当前用户身份污染后续测试。
     */
    @AfterEach
    void clearLoginUserContext() {
        LoginUserContext.clear();
    }

    /**
     * 删除普通用户时应逻辑删除用户、清理角色关系并清理该用户会话。
     */
    @Test
    void removeUserShouldMarkDeletedAndClearRoleRelations() {
        SysUser user = createUser(57L, "delete-test");
        when(sysUserMapper.selectById(user.getId())).thenReturn(user);
        when(sysRoleMapper.selectRoleCodesByUserId(user.getId())).thenReturn(Collections.singletonList("SYSTEM_ADMIN"));

        userService.removeUser(user.getId());

        assertEquals(1, user.getDeleted());
        verify(sysUserRoleMapper).delete(anyUserRoleWrapper());
        verify(sysUserMapper).updateById(user);
        verify(loginSessionManager).invalidateUserSession(user.getId());
    }

    /**
     * 当前登录用户不能删除自己。
     */
    @Test
    void removeUserShouldRejectDeletingCurrentLoginUser() {
        SysUser user = createUser(58L, "self-delete-test");
        when(sysUserMapper.selectById(user.getId())).thenReturn(user);
        setCurrentUser(user.getId(), user.getUsername(), Collections.singletonList("SYSTEM_ADMIN"));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.removeUser(user.getId()));

        assertEquals("当前登录用户不允许删除", exception.getMessage());
    }

    /**
     * 超级管理员账号不能被删除。
     */
    @Test
    void removeUserShouldRejectDeletingSuperAdmin() {
        SysUser user = createUser(59L, "super-delete-test");
        when(sysUserMapper.selectById(user.getId())).thenReturn(user);
        when(sysRoleMapper.selectRoleCodesByUserId(user.getId())).thenReturn(Collections.singletonList("SUPER_ADMIN"));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.removeUser(user.getId()));

        assertEquals("超级管理员不允许删除", exception.getMessage());
    }

    /**
     * 用户状态变化后应立即清理该用户会话。
     */
    @Test
    void changeStatusShouldInvalidateUserSession() {
        SysUser user = createUser(60L, "status-test");
        when(sysUserMapper.selectById(user.getId())).thenReturn(user);

        userService.changeStatus(user.getId(), 0);

        verify(loginSessionManager).invalidateUserSession(user.getId());
    }

    /**
     * 重置密码后应立即清理该用户会话。
     */
    @Test
    void resetPasswordShouldInvalidateUserSession() {
        SysUser user = createUser(61L, "reset-password-test");
        when(sysUserMapper.selectById(user.getId())).thenReturn(user);
        when(passwordEncoder.encode("NewPass@123456")).thenReturn("encoded-password");

        userService.resetPassword(user.getId(), "NewPass@123456");

        assertEquals("encoded-password", user.getPassword());
        verify(loginSessionManager).invalidateUserSession(user.getId());
    }

    /**
     * 角色分配变化后应立即清理该用户会话，避免继续使用旧权限缓存。
     */
    @Test
    void assignRolesShouldInvalidateUserSession() {
        Long userId = 62L;

        userService.assignRoles(userId, Collections.emptyList());

        verify(sysUserRoleMapper).delete(anyUserRoleWrapper());
        verify(loginSessionManager).invalidateUserSession(eq(userId));
    }

    /**
     * 构造用户实体，减少测试中与断言无关的字段噪音。
     */
    private SysUser createUser(Long id, String username) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setDeptId(1L);
        user.setUsername(username);
        user.setPassword("test");
        user.setNickname("Delete Test");
        user.setStatus(1);
        user.setDeleted(0);
        return user;
    }

    /**
     * 匹配用户角色查询条件，集中处理泛型参数以避免测试编译告警。
     */
    private Wrapper<SysUserRole> anyUserRoleWrapper() {
        return any();
    }

    /**
     * 设置当前登录用户上下文，用于验证自删保护逻辑。
     */
    private void setCurrentUser(Long userId, String username, java.util.List<String> roles) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setUsername(username);
        loginUser.setRoles(roles);
        LoginUserContext.set(loginUser);
    }
}
