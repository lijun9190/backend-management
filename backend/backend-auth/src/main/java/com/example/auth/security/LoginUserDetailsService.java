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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .disabled(user.getStatus() == null || user.getStatus() == 0)
                .authorities("ROLE_USER")
                .build();
    }
}
