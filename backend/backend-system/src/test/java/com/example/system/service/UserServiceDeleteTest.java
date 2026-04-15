package com.example.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.context.LoginUserContext;
import com.example.common.entity.SysUser;
import com.example.common.entity.SysUserRole;
import com.example.common.exception.BusinessException;
import com.example.common.model.security.LoginUser;
import com.example.system.BackendSystemApplication;
import com.example.system.mapper.SysUserMapper;
import com.example.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = BackendSystemApplication.class)
class UserServiceDeleteTest {

    @Autowired
    private UserService userService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @AfterEach
    void clearLoginUserContext() {
        LoginUserContext.clear();
    }

    @Test
    @Transactional
    void removeUserShouldMarkDeletedAndClearRoleRelations() throws Exception {
        SysUser user = new SysUser();
        user.setDeptId(1L);
        user.setUsername("delete-test-" + System.currentTimeMillis());
        user.setPassword("test");
        user.setNickname("Delete Test");
        user.setStatus(1);
        user.setDeleted(0);
        user.setCreateBy("test");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateBy("test");
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(user);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(2L);
        sysUserRoleMapper.insert(userRole);

        Method removeUser = userService.getClass().getMethod("removeUser", Long.class);
        removeUser.invoke(userService, user.getId());

        SysUser deletedUser = sysUserMapper.selectById(user.getId());
        assertNotNull(deletedUser, "expected logical delete to keep the user row");
        assertEquals(1, deletedUser.getDeleted(), "expected user.deleted to be marked as 1");

        Long relationCount = sysUserRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId())
        );
        assertEquals(0L, relationCount, "expected user-role relations to be removed during delete");
    }

    @Test
    @Transactional
    void removeUserShouldRejectDeletingCurrentLoginUser() throws Exception {
        SysUser user = createUser("self-delete-test-" + System.currentTimeMillis());
        setCurrentUser(user.getId(), user.getUsername(), Collections.singletonList("SYSTEM_ADMIN"));

        Method removeUser = userService.getClass().getMethod("removeUser", Long.class);
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> removeUser.invoke(userService, user.getId()));

        assertEquals(BusinessException.class, exception.getTargetException().getClass());
        assertEquals("当前登录用户不允许删除", exception.getTargetException().getMessage());
    }

    @Test
    @Transactional
    void removeUserShouldRejectDeletingSuperAdmin() throws Exception {
        SysUser user = createUser("super-delete-test-" + System.currentTimeMillis());

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(1L);
        sysUserRoleMapper.insert(userRole);

        Method removeUser = userService.getClass().getMethod("removeUser", Long.class);
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> removeUser.invoke(userService, user.getId()));

        assertEquals(BusinessException.class, exception.getTargetException().getClass());
        assertEquals("超级管理员不允许删除", exception.getTargetException().getMessage());
    }

    private SysUser createUser(String username) {
        SysUser user = new SysUser();
        user.setDeptId(1L);
        user.setUsername(username);
        user.setPassword("test");
        user.setNickname("Delete Test");
        user.setStatus(1);
        user.setDeleted(0);
        user.setCreateBy("test");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateBy("test");
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(user);
        return user;
    }

    private void setCurrentUser(Long userId, String username, java.util.List<String> roles) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setUsername(username);
        loginUser.setRoles(roles);
        LoginUserContext.set(loginUser);
    }
}
