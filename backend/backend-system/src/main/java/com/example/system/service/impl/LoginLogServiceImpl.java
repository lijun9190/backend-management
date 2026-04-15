package com.example.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.entity.SysLoginLog;
import com.example.common.model.result.PageResult;
import com.example.system.dto.log.LoginLogQueryDTO;
import com.example.system.mapper.SysLoginLogMapper;
import com.example.system.service.LoginLogService;
import com.example.system.vo.log.LoginLogPageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

/**
 * 登录日志服务实现。
 */
@Service
public class LoginLogServiceImpl implements LoginLogService {

    private final SysLoginLogMapper sysLoginLogMapper;

    public LoginLogServiceImpl(SysLoginLogMapper sysLoginLogMapper) {
        this.sysLoginLogMapper = sysLoginLogMapper;
    }

    @Override
    public PageResult<LoginLogPageVO> pageQuery(LoginLogQueryDTO dto) {
        Page<SysLoginLog> page = new Page<>(dto.getCurrent(), dto.getSize());
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(dto.getUsername()), SysLoginLog::getUsername, dto.getUsername())
                .eq(dto.getLoginStatus() != null, SysLoginLog::getLoginStatus, dto.getLoginStatus())
                .orderByDesc(SysLoginLog::getLoginTime);
        Page<SysLoginLog> result = sysLoginLogMapper.selectPage(page, wrapper);
        PageResult<LoginLogPageVO> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords().stream().map(item -> {
            LoginLogPageVO vo = new LoginLogPageVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).collect(Collectors.toList()));
        pageResult.setCurrent(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setPages(result.getPages());
        return pageResult;
    }
}
