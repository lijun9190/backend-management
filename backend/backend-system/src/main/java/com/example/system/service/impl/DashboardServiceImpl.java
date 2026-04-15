package com.example.system.service.impl;

import com.example.common.entity.SysLoginLog;
import com.example.system.mapper.SysLoginLogMapper;
import com.example.system.mapper.SysMenuMapper;
import com.example.system.mapper.SysRoleMapper;
import com.example.system.mapper.SysUserMapper;
import com.example.system.service.DashboardService;
import com.example.system.vo.dashboard.DashboardOverviewVO;
import com.example.system.vo.dashboard.DashboardStatVO;
import com.example.system.vo.log.LoginLogPageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 仪表盘服务实现。
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysLoginLogMapper sysLoginLogMapper;

    public DashboardServiceImpl(SysUserMapper sysUserMapper,
                                SysRoleMapper sysRoleMapper,
                                SysMenuMapper sysMenuMapper,
                                SysLoginLogMapper sysLoginLogMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysMenuMapper = sysMenuMapper;
        this.sysLoginLogMapper = sysLoginLogMapper;
    }

    @Override
    public DashboardOverviewVO overview() {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        Long userCount = sysUserMapper.countAllUsers();
        Long roleCount = sysRoleMapper.countAllRoles();
        Long menuCount = sysMenuMapper.countAllMenus();
        Long recentLoginCount = sysLoginLogMapper.countRecentSevenDays();
        vo.setUserCount(userCount);
        vo.setRoleCount(roleCount);
        vo.setMenuCount(menuCount);
        vo.setRecentLoginCount(recentLoginCount);
        vo.setCards(Arrays.asList(
                new DashboardStatVO("用户总数", userCount),
                new DashboardStatVO("角色总数", roleCount),
                new DashboardStatVO("菜单总数", menuCount),
                new DashboardStatVO("近 7 天登录次数", recentLoginCount)
        ));
        vo.setRecentLoginLogs(sysLoginLogMapper.selectRecentList().stream().map(item -> {
            LoginLogPageVO logVO = new LoginLogPageVO();
            BeanUtils.copyProperties(item, logVO);
            return logVO;
        }).collect(Collectors.toList()));
        return vo;
    }
}
