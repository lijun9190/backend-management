package com.example.auth.service;

import com.example.auth.BackendAuthApplication;
import com.example.auth.dto.LoginDTO;
import com.example.auth.mapper.SysUserMapper;
import com.example.common.entity.SysUser;
import com.example.common.exception.BusinessException;
import com.example.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = BackendAuthApplication.class)
class AuthServiceLoginRoleGuardTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @Transactional
    void loginShouldRejectUserWithoutRoles() {
        SysUser user = new SysUser();
        user.setDeptId(1L);
        user.setUsername("no-role-login-" + System.currentTimeMillis());
        user.setPassword(passwordEncoder.encode("Admin@123456"));
        user.setNickname("No Role");
        user.setStatus(1);
        user.setDeleted(0);
        user.setCreateBy("test");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateBy("test");
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(user);

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
