package com.example.auth.service;

import com.example.auth.dto.LoginDTO;
import com.example.auth.mapper.AuthMenuMapper;
import com.example.auth.mapper.AuthRoleMapper;
import com.example.auth.mapper.SysDeptMapper;
import com.example.auth.mapper.SysLoginLogMapper;
import com.example.auth.mapper.SysUserMapper;
import com.example.auth.service.impl.AuthServiceImpl;
import com.example.common.entity.SysUser;
import com.example.common.exception.BusinessException;
import com.example.common.security.LoginSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 登录角色保护测试，使用Mock隔离数据库和Redis依赖。
 */
class AuthServiceLoginRoleGuardTest {

    private AuthService authService;
    private SysUserMapper sysUserMapper;
    private AuthRoleMapper authRoleMapper;
    private PasswordEncoder passwordEncoder;

    /**
     * 初始化认证服务及其依赖，保证登录校验测试不依赖真实数据库。
     */
    @BeforeEach
    void setUp() {
        sysUserMapper = mock(SysUserMapper.class);
        SysDeptMapper sysDeptMapper = mock(SysDeptMapper.class);
        SysLoginLogMapper sysLoginLogMapper = mock(SysLoginLogMapper.class);
        authRoleMapper = mock(AuthRoleMapper.class);
        AuthMenuMapper authMenuMapper = mock(AuthMenuMapper.class);
        LoginSessionManager loginSessionManager = mock(LoginSessionManager.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authService = new AuthServiceImpl(
                sysUserMapper,
                sysDeptMapper,
                sysLoginLogMapper,
                authRoleMapper,
                authMenuMapper,
                loginSessionManager,
                passwordEncoder
        );
    }

    /**
     * 用户没有任何角色时应拒绝登录，并返回清晰的业务错误。
     */
    @Test
    void loginShouldRejectUserWithoutRoles() {
        SysUser user = new SysUser();
        user.setId(100L);
        user.setDeptId(1L);
        user.setUsername("no-role-login");
        user.setPassword("encoded-password");
        user.setNickname("No Role");
        user.setStatus(1);
        user.setDeleted(0);
        when(sysUserMapper.selectByUsername(user.getUsername())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(authRoleMapper.selectRoleCodesByUserId(user.getId())).thenReturn(Collections.emptyList());

        LoginDTO dto = new LoginDTO();
        dto.setUsername(user.getUsername());
        dto.setPassword("Admin@123456");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(dto, new MockHttpServletRequest())
        );

        assertEquals("当前账号未分配角色，请联系管理员", exception.getMessage());
    }
}
