package com.example.auth.security;

import com.example.auth.mapper.SysUserMapper;
import com.example.common.entity.SysUser;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security 用户加载服务。
 */
@Service
public class LoginUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    public LoginUserDetailsService(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    /**
     * 根据用户名加载用户信息
     * @param username 用户名
     * @return UserDetails 用户详细信息
     * @throws UsernameNotFoundException 当用户不存在时抛出此异常
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 通过用户名查询用户信息
        SysUser user = sysUserMapper.selectByUsername(username);
        // 如果用户不存在，抛出用户不存在异常
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        // 构建并返回UserDetails对象，包含用户名、密码、状态和权限信息
        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .disabled(user.getStatus() == null || user.getStatus() == 0) // 根据用户状态设置是否禁用
                .authorities("ROLE_USER") // 设置用户权限
                .build();
    }
}
