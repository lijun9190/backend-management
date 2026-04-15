package com.example.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.auth.dto.LoginDTO;
import com.example.auth.dto.UpdatePasswordDTO;
import com.example.auth.mapper.AuthMenuMapper;
import com.example.auth.mapper.AuthRoleMapper;
import com.example.auth.mapper.SysDeptMapper;
import com.example.auth.mapper.SysLoginLogMapper;
import com.example.auth.mapper.SysUserMapper;
import com.example.auth.service.AuthService;
import com.example.auth.vo.LoginVO;
import com.example.auth.vo.UserMenuVO;
import com.example.auth.vo.UserProfileVO;
import com.example.common.constant.CommonConstants;
import com.example.common.constant.RedisKeyConstants;
import com.example.common.context.LoginUserContext;
import com.example.common.entity.SysDept;
import com.example.common.entity.SysLoginLog;
import com.example.common.entity.SysMenu;
import com.example.common.entity.SysUser;
import com.example.common.exception.BusinessException;
import com.example.common.model.security.LoginUser;
import com.example.common.redis.RedisOperator;
import com.example.common.security.JwtTokenProvider;
import com.example.common.util.IpUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 认证服务实现。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final AuthRoleMapper authRoleMapper;
    private final AuthMenuMapper authMenuMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisOperator redisOperator;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           SysDeptMapper sysDeptMapper,
                           SysLoginLogMapper sysLoginLogMapper,
                           AuthRoleMapper authRoleMapper,
                           AuthMenuMapper authMenuMapper,
                           JwtTokenProvider jwtTokenProvider,
                           RedisOperator redisOperator,
                           PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.sysDeptMapper = sysDeptMapper;
        this.sysLoginLogMapper = sysLoginLogMapper;
        this.authRoleMapper = authRoleMapper;
        this.authMenuMapper = authMenuMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisOperator = redisOperator;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO dto, HttpServletRequest request) {
        SysUser user = sysUserMapper.selectByUsername(dto.getUsername());
        if (user == null || Integer.valueOf(CommonConstants.DELETED_YES).equals(user.getDeleted())) {
            recordLoginLog(dto.getUsername(), null, request, 0, "用户名不存在");
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() == CommonConstants.STATUS_DISABLED) {
            recordLoginLog(dto.getUsername(), user.getNickname(), request, 0, "账号已被禁用");
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            recordLoginLog(dto.getUsername(), user.getNickname(), request, 0, "密码错误");
            throw new BusinessException("用户名或密码错误");
        }

        LoginUser loginUser;
        try {
            loginUser = buildLoginUser(user);
        } catch (BusinessException exception) {
            recordLoginLog(dto.getUsername(), user.getNickname(), request, 0, exception.getMessage());
            throw exception;
        }
        String token = jwtTokenProvider.createToken(loginUser);
        cacheLoginToken(token, loginUser);
        recordLoginLog(dto.getUsername(), user.getNickname(), request, 1, "登录成功");
        return new LoginVO(token, "Bearer", jwtTokenProvider.getExpireSeconds());
    }

    @Override
    public void logout(String authorization) {
        String token = resolveToken(authorization);
        if (StringUtils.hasText(token)) {
            redisOperator.delete(RedisKeyConstants.loginTokenKey(token));
        }
    }

    @Override
    public UserProfileVO getCurrentUserProfile() {
        LoginUser loginUser = currentLoginUser();
        UserProfileVO profileVO = new UserProfileVO();
        profileVO.setUserId(loginUser.getUserId());
        profileVO.setUsername(loginUser.getUsername());
        profileVO.setNickname(loginUser.getNickname());
        profileVO.setDeptName(loginUser.getDeptName());
        profileVO.setRoles(loginUser.getRoles());
        profileVO.setPermissions(loginUser.getPermissions());

        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        if (user != null) {
            profileVO.setRealName(user.getRealName());
            profileVO.setPhone(user.getPhone());
            profileVO.setEmail(user.getEmail());
            profileVO.setAvatar(user.getAvatar());
        }
        List<SysMenu> menus = loadMenus(loginUser.getUserId(), loginUser.isSuperAdmin());
        profileVO.setMenus(buildMenuTree(menus, 0L));
        return profileVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(UpdatePasswordDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        LoginUser loginUser = currentLoginUser();
        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new BusinessException("当前用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码输入错误");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setUpdateBy(loginUser.getUsername());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    /**
     * 组装当前登录用户信息。
     */
    private LoginUser buildLoginUser(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setStatus(user.getStatus());
        SysDept dept = user.getDeptId() == null ? null : sysDeptMapper.selectById(user.getDeptId());
        loginUser.setDeptName(dept == null ? null : dept.getDeptName());

        List<String> roleCodes = authRoleMapper.selectRoleCodesByUserId(user.getId());
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BusinessException("当前账号未分配角色，请联系管理员");
        }
        loginUser.setRoles(roleCodes);
        if (loginUser.isSuperAdmin()) {
            loginUser.setPermissions(authMenuMapper.selectAllMenus().stream()
                    .map(SysMenu::getPermissionCode)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .collect(Collectors.toList()));
        } else {
            loginUser.setPermissions(authMenuMapper.selectPermissionCodesByUserId(user.getId()));
        }
        return loginUser;
    }

    private void cacheLoginToken(String token, LoginUser loginUser) {
        redisOperator.set(RedisKeyConstants.loginTokenKey(token), loginUser,
                jwtTokenProvider.getExpireSeconds(), TimeUnit.SECONDS);
        redisOperator.set(RedisKeyConstants.loginUserKey(loginUser.getUserId()), token,
                jwtTokenProvider.getExpireSeconds(), TimeUnit.SECONDS);
    }

    private void recordLoginLog(String username, String nickname, HttpServletRequest request, Integer status, String message) {
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setUsername(username);
        loginLog.setNickname(nickname);
        loginLog.setLoginIp(IpUtils.getIp(request));
        loginLog.setBrowser(parseBrowser(request));
        loginLog.setOs(parseOs(request));
        loginLog.setLoginStatus(status);
        loginLog.setMessage(message);
        loginLog.setLoginTime(LocalDateTime.now());
        sysLoginLogMapper.insert(loginLog);
    }

    private String parseBrowser(HttpServletRequest request) {
        String ua = request == null ? null : request.getHeader("User-Agent");
        if (!StringUtils.hasText(ua)) {
            return "Unknown";
        }
        if (ua.contains("Edg")) {
            return "Edge";
        }
        if (ua.contains("Chrome")) {
            return "Chrome";
        }
        if (ua.contains("Firefox")) {
            return "Firefox";
        }
        return "Browser";
    }

    private String parseOs(HttpServletRequest request) {
        String ua = request == null ? null : request.getHeader("User-Agent");
        if (!StringUtils.hasText(ua)) {
            return "Unknown";
        }
        if (ua.contains("Windows")) {
            return "Windows";
        }
        if (ua.contains("Mac OS")) {
            return "macOS";
        }
        if (ua.contains("Linux")) {
            return "Linux";
        }
        return "OS";
    }

    private List<SysMenu> loadMenus(Long userId, boolean superAdmin) {
        return superAdmin ? authMenuMapper.selectAllMenus() : authMenuMapper.selectMenusByUserId(userId);
    }

    private List<UserMenuVO> buildMenuTree(List<SysMenu> menus, Long parentId) {
        List<UserMenuVO> tree = new ArrayList<>();
        List<SysMenu> current = menus.stream()
                .filter(item -> {
                    Long currentParentId = item.getParentId() == null ? 0L : item.getParentId();
                    return currentParentId.equals(parentId);
                })
                .sorted(Comparator.comparing(SysMenu::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SysMenu::getId))
                .collect(Collectors.toList());
        for (SysMenu menu : current) {
            UserMenuVO node = new UserMenuVO();
            node.setId(menu.getId());
            node.setParentId(menu.getParentId());
            node.setName(menu.getMenuName());
            node.setMenuType(menu.getMenuType());
            node.setPath(menu.getPath());
            node.setComponent(menu.getComponent());
            node.setRouteName(menu.getRouteName());
            node.setIcon(menu.getIcon());
            node.setSort(menu.getSort());
            node.setPermissionCode(menu.getPermissionCode());
            node.setVisible(menu.getVisible());
            node.setStatus(menu.getStatus());
            node.setChildren(buildMenuTree(menus, menu.getId()));
            tree.add(node);
        }
        return tree;
    }

    private LoginUser currentLoginUser() {
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            throw new BusinessException(CommonConstants.UNAUTHORIZED_CODE, "登录状态已失效");
        }
        return loginUser;
    }

    private String resolveToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        if (authorization.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return authorization.substring(CommonConstants.TOKEN_PREFIX.length());
        }
        return authorization;
    }
}
