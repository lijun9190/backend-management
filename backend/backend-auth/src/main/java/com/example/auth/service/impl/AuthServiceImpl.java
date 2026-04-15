package com.example.auth.service.impl;

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

/**
 * 登录方法，处理用户登录逻辑
 * @param dto 登录数据传输对象，包含用户名和密码
 * @param request HTTP请求对象，用于获取客户端信息
 * @return LoginVO 包含token和过期时间的登录结果对象
 * @throws BusinessException 当登录失败时抛出业务异常
 */
    @Override
    @Transactional(rollbackFor = Exception.class) // 事务注解，确保方法内所有操作要么全部成功，要么全部回滚
    public LoginVO login(LoginDTO dto, HttpServletRequest request) {
    // 根据用户名查询用户
        SysUser user = sysUserMapper.selectByUsername(dto.getUsername());
    // 判断用户是否存在或已被逻辑删除
        if (user == null || Integer.valueOf(CommonConstants.DELETED_YES).equals(user.getDeleted())) {
        // 记录登录日志（用户名不存在）
            recordLoginLog(dto.getUsername(), null, request, 0, "用户名不存在");
        // 抛出用户名或密码错误异常
            throw new BusinessException("用户名或密码错误");
        }
    // 检查用户状态是否被禁用
        if (user.getStatus() == null || user.getStatus() == CommonConstants.STATUS_DISABLED) {
        // 记录登录日志（账号被禁用）
            recordLoginLog(dto.getUsername(), user.getNickname(), request, 0, "账号已被禁用");
        // 抛出账号被禁用异常
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
    // 验证密码是否正确
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
        // 记录登录日志（密码错误）
            recordLoginLog(dto.getUsername(), user.getNickname(), request, 0, "密码错误");
        // 抛出用户名或密码错误异常
            throw new BusinessException("用户名或密码错误");
        }

    // 构建登录用户对象
        LoginUser loginUser;
        try {
            loginUser = buildLoginUser(user);
        } catch (BusinessException exception) {
        // 记录登录日志（构建登录用户失败）
            recordLoginLog(dto.getUsername(), user.getNickname(), request, 0, exception.getMessage());
            throw exception;
        }
    // 生成JWT token
        String token = jwtTokenProvider.createToken(loginUser);
    // 缓存登录token
        cacheLoginToken(token, loginUser);
    // 记录登录日志（登录成功）
        recordLoginLog(dto.getUsername(), user.getNickname(), request, 1, "登录成功");
    // 返回登录结果对象，包含token和过期时间
        return new LoginVO(token, "Bearer", jwtTokenProvider.getExpireSeconds());
    }

    @Override
    public void logout(String authorization) {
        String token = resolveToken(authorization);
        if (StringUtils.hasText(token)) {
            redisOperator.delete(RedisKeyConstants.loginTokenKey(token));
        }
    }

/**
 * 获取当前登录用户的个人信息
 * 该方法会从登录用户信息中提取基本数据，并从数据库中获取更多详细信息
 *
 * @return UserProfileVO 包含用户完整信息的视图对象
 */
    @Override
    public UserProfileVO getCurrentUserProfile() {
    // 获取当前登录用户信息
        LoginUser loginUser = currentLoginUser();
    // 创建用户信息视图对象
        UserProfileVO profileVO = new UserProfileVO();
    // 设置从登录用户信息中获取的基本字段
        profileVO.setUserId(loginUser.getUserId());
        profileVO.setUsername(loginUser.getUsername());
        profileVO.setNickname(loginUser.getNickname());
        profileVO.setDeptName(loginUser.getDeptName());
        profileVO.setRoles(loginUser.getRoles());
        profileVO.setPermissions(loginUser.getPermissions());

    // 根据用户ID查询系统用户详细信息
        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
    // 如果用户信息存在，则设置更多详细信息
        if (user != null) {
            profileVO.setRealName(user.getRealName());
            profileVO.setPhone(user.getPhone());
            profileVO.setEmail(user.getEmail());
            profileVO.setAvatar(user.getAvatar());
        }
    // 加载用户的菜单权限
        List<SysMenu> menus = loadMenus(loginUser.getUserId(), loginUser.isSuperAdmin());
    // 构建菜单树并设置到用户信息中
        profileVO.setMenus(buildMenuTree(menus, 0L));
    // 返回完整的用户信息视图对象
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
    // 获取用户角色代码
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

/**
 * 将登录令牌和用户信息缓存到Redis中
 * 该方法实现了双向缓存，即同时缓存token到用户信息和用户ID到token的映射关系
 *
 * @param token 登录令牌
 * @param loginUser 登录用户信息对象
 */
    private void cacheLoginToken(String token, LoginUser loginUser) {
    // 将token作为key，登录用户信息作为value存入Redis
    // 设置过期时间为JWT令牌的有效期（以秒为单位）
        redisOperator.set(RedisKeyConstants.loginTokenKey(token), loginUser,
                jwtTokenProvider.getExpireSeconds(), TimeUnit.SECONDS);
    // 将用户ID作为key，token作为value存入Redis
    // 设置过期时间为JWT令牌的有效期（以秒为单位）
    // 这样可以通过用户ID快速找到对应的token
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

/**
 * 根据用户ID和是否超级管理员标志加载菜单列表
 * @param userId 用户ID
 * @param superAdmin 是否为超级管理员标志
 * @return 返回菜单列表，如果是超级管理员则返回所有菜单，否则返回该用户有权限的菜单
 */
    private List<SysMenu> loadMenus(Long userId, boolean superAdmin) {
        return superAdmin ? authMenuMapper.selectAllMenus() : authMenuMapper.selectMenusByUserId(userId);
    // 使用三元运算符判断是否为超级管理员
    // 如果是超级管理员，调用selectAllMenus()获取所有菜单
    // 否则，调用selectMenusByUserId(userId)获取指定用户的菜单
    }

/**
 * 构建菜单树形结构
 * @param menus 所有菜单列表
 * @param parentId 父菜单ID
 * @return 构建好的菜单树形结构列表
 */
    private List<UserMenuVO> buildMenuTree(List<SysMenu> menus, Long parentId) {
    // 创建树形结构列表
        List<UserMenuVO> tree = new ArrayList<>();
    // 筛选出当前父级ID下的所有菜单，并按排序号和ID排序
        List<SysMenu> current = menus.stream()
                .filter(item -> {
                // 处理父ID为null的情况，默认为0
                    Long currentParentId = item.getParentId() == null ? 0L : item.getParentId();
                // 过滤出当前父级ID下的菜单
                    return currentParentId.equals(parentId);
                })
            // 先按排序号排序，null值排在最后，再按ID排序
                .sorted(Comparator.comparing(SysMenu::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SysMenu::getId))
                .collect(Collectors.toList());
    // 遍历当前层级的菜单
        for (SysMenu menu : current) {
        // 创建菜单节点对象
            UserMenuVO node = new UserMenuVO();
        // 设置菜单节点的基本属性
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
        // 将节点添加到树形结构中
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
