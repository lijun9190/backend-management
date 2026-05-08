package com.example.auth.service.impl;

import com.example.auth.dto.LoginDTO;
import com.example.auth.dto.RefreshTokenDTO;
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
import com.example.common.context.LoginUserContext;
import com.example.common.entity.SysDept;
import com.example.common.entity.SysLoginLog;
import com.example.common.entity.SysMenu;
import com.example.common.entity.SysUser;
import com.example.common.exception.BusinessException;
import com.example.common.model.security.AuthTokens;
import com.example.common.model.security.LoginUser;
import com.example.common.security.LoginSessionManager;
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
import java.util.stream.Collectors;

/**
 * 认证服务实现类
 * 提供用户登录、登出、刷新令牌、获取用户信息、更新密码等功能
 */
@Service
public class AuthServiceImpl implements AuthService {

    // 依赖注入的Mapper和Service
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final AuthRoleMapper authRoleMapper;
    private final AuthMenuMapper authMenuMapper;
    private final LoginSessionManager loginSessionManager;
    private final PasswordEncoder passwordEncoder;

    /**
     * 构造函数，注入所有依赖
     */
    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           SysDeptMapper sysDeptMapper,
                           SysLoginLogMapper sysLoginLogMapper,
                           AuthRoleMapper authRoleMapper,
                           AuthMenuMapper authMenuMapper,
                           LoginSessionManager loginSessionManager,
                           PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.sysDeptMapper = sysDeptMapper;
        this.sysLoginLogMapper = sysLoginLogMapper;
        this.authRoleMapper = authRoleMapper;
        this.authMenuMapper = authMenuMapper;
        this.loginSessionManager = loginSessionManager;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户登录方法
     * @param dto 登录数据传输对象
     * @param request HTTP请求对象
     * @return 登录结果视图对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO dto, HttpServletRequest request) {
        // 根据用户名查询用户
        SysUser user = sysUserMapper.selectByUsername(dto.getUsername());
        // 判断用户是否存在或已被删除
        if (user == null || Integer.valueOf(CommonConstants.DELETED_YES).equals(user.getDeleted())) {
            recordLoginLog(dto.getUsername(), null, request, 0, "用户名不存在");
            throw new BusinessException("用户名或密码错误");
        }
        // 判断用户是否被禁用
        if (user.getStatus() == null || user.getStatus().equals(CommonConstants.STATUS_DISABLED)) {
            recordLoginLog(dto.getUsername(), user.getNickname(), request, 0, "账号已被禁用");
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        // 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            recordLoginLog(dto.getUsername(), user.getNickname(), request, 0, "密码错误");
            throw new BusinessException("用户名或密码错误");
        }

        // 构建登录用户对象
        LoginUser loginUser;
        try {
            loginUser = buildLoginUser(user);
        } catch (BusinessException exception) {
            recordLoginLog(dto.getUsername(), user.getNickname(), request, 0, exception.getMessage());
            throw exception;
        }

        // 创建会话并生成令牌
        AuthTokens authTokens = loginSessionManager.createSession(loginUser);
        // 记录登录日志
        recordLoginLog(dto.getUsername(), user.getNickname(), request, 1, "登录成功");
        return toLoginVO(authTokens);
    }

    /**
     * 刷新访问令牌
     * @param dto 刷新令牌数据传输对象
     * @return 登录结果视图对象
     */
    @Override
    public LoginVO refreshToken(RefreshTokenDTO dto) {
        return toLoginVO(loginSessionManager.refreshSession(dto.getRefreshToken()));
    }

    /**
     * 用户登出方法
     * @param authorization 认证信息
     */
    @Override
    public void logout(String authorization) {
        String token = resolveToken(authorization);
        if (StringUtils.hasText(token)) {
            loginSessionManager.invalidateAccessToken(token);
        }
    }

    /**
     * 获取当前用户个人信息
     * @return 用户个人信息视图对象
     */
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

        // 获取用户详细信息
        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        if (user != null) {
            profileVO.setRealName(user.getRealName());
            profileVO.setPhone(user.getPhone());
            profileVO.setEmail(user.getEmail());
            profileVO.setAvatar(user.getAvatar());
        }
        // 加载用户菜单并构建菜单树
        List<SysMenu> menus = loadMenus(loginUser.getUserId(), loginUser.isSuperAdmin());
        profileVO.setMenus(buildMenuTree(menus, 0L));
        return profileVO;
    }

    /**
     * 更新用户密码
     * @param dto 更新密码数据传输对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(UpdatePasswordDTO dto) {
        // 验证两次输入的新密码是否一致
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        LoginUser loginUser = currentLoginUser();
        // 获取当前用户信息
        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new BusinessException("当前用户不存在");
        }
        // 验证旧密码是否正确
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码输入错误");
        }
        // 更新用户密码
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setUpdateBy(loginUser.getUsername());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    /**
     * 构建登录用户对象
     * @param user 系统用户实体
     * @return 登录用户对象
     */
    private LoginUser buildLoginUser(SysUser user) {
        // 创建登录用户对象实例
        LoginUser loginUser = new LoginUser();
        // 设置用户基本信息
        loginUser.setUserId(user.getId());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setStatus(user.getStatus());
        // 获取用户部门信息
        // 如果用户部门ID为空，则部门信息为null，否则查询数据库获取部门信息
        SysDept dept = user.getDeptId() == null ? null : sysDeptMapper.selectById(user.getDeptId());
        // 设置用户部门名称
        loginUser.setDeptName(dept == null ? null : dept.getDeptName());

        // 获取用户角色编码列表
        // 根据用户ID查询其拥有的所有角色编码
        List<String> roleCodes = authRoleMapper.selectRoleCodesByUserId(user.getId());
        // 检查用户是否拥有角色
        if (roleCodes == null || roleCodes.isEmpty()) {
            // 如果用户没有分配任何角色，抛出业务异常
            throw new BusinessException("当前账号未分配角色，请联系管理员");
        }
        // 设置用户角色列表
        loginUser.setRoles(roleCodes);
        // 根据用户角色获取权限列表
        // 判断用户是否为超级管理员
        if (loginUser.isSuperAdmin()) {
            // 如果是超级管理员，获取所有菜单权限
            loginUser.setPermissions(authMenuMapper.selectAllMenus().stream()
                    // 提取菜单权限编码
                    .map(SysMenu::getPermissionCode)
                    // 过滤掉空权限编码
                    .filter(StringUtils::hasText)
                    // 去重
                    .distinct()
                    // 收集为List集合
                    .collect(Collectors.toList()));
        } else {
            // 如果不是超级管理员，只获取用户自身角色对应的权限
            loginUser.setPermissions(authMenuMapper.selectPermissionCodesByUserId(user.getId()));
        }
        // 返回构建完成的登录用户对象
        return loginUser;
    }

    /**
     * 记录用户登录日志
     * @param username 用户名
     * @param nickname 昵称
     * @param request HTTP请求对象
     * @param status 登录状态
     * @param message 登录消息
     */
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

    /**
     * 解析浏览器信息
     * @param request HTTP请求对象
     * @return 浏览器名称
     */
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

    /**
     * 解析操作系统信息
     * @param request HTTP请求对象
     * @return 操作系统名称
     */
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

    /**
     * 加载用户菜单
     * @param userId 用户ID
     * @param superAdmin 是否为超级管理员
     * @return 菜单列表
     */
    private List<SysMenu> loadMenus(Long userId, boolean superAdmin) {
        return superAdmin ? authMenuMapper.selectAllMenus() : authMenuMapper.selectMenusByUserId(userId);
    }

    /**
     * 构建菜单树
     * @param menus 菜单列表
     * @param parentId 父菜单ID
     * @return 菜单树结构
     */
    private List<UserMenuVO> buildMenuTree(List<SysMenu> menus, Long parentId) {
        List<UserMenuVO> tree = new ArrayList<>();
        // 筛选出当前父菜单下的所有子菜单
        List<SysMenu> current = menus.stream()
                .filter(item -> {
                    Long currentParentId = item.getParentId() == null ? 0L : item.getParentId();
                    return currentParentId.equals(parentId);
                })
                // 按排序号和ID排序
                .sorted(Comparator.comparing(SysMenu::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SysMenu::getId))
                .collect(Collectors.toList());
        // 遍历当前菜单列表，构建菜单树节点
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
            // 递归构建子菜单
            node.setChildren(buildMenuTree(menus, menu.getId()));
            tree.add(node);
        }
        return tree;
    }

    /**
     * 获取当前登录用户
     * @return 登录用户对象
     */
    private LoginUser currentLoginUser() {
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            throw new BusinessException(CommonConstants.UNAUTHORIZED_CODE, "登录状态已失效");
        }
        return loginUser;
    }

    /**
     * 解析令牌
     * @param authorization 认证信息
     * @return 令牌字符串
     */
    private String resolveToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        if (authorization.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return authorization.substring(CommonConstants.TOKEN_PREFIX.length());
        }
        return authorization;
    }

    /**
     * 转换为登录视图对象
     * @param authTokens 认证令牌对象
     * @return 登录视图对象
     */
    private LoginVO toLoginVO(AuthTokens authTokens) {
        return new LoginVO(
                authTokens.getAccessToken(),
                authTokens.getRefreshToken(),
                authTokens.getTokenType(),
                authTokens.getAccessExpireIn(),
                authTokens.getRefreshExpireIn()
        );
    }
}
