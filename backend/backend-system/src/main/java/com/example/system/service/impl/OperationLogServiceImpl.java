package com.example.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.context.LoginUserContext;
import com.example.common.entity.SysOperationLog;
import com.example.common.model.result.PageResult;
import com.example.common.model.security.LoginUser;
import com.example.system.dto.log.OperationLogQueryDTO;
import com.example.system.mapper.SysOperationLogMapper;
import com.example.system.service.OperationLogService;
import com.example.system.vo.log.OperationLogPageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 操作日志服务实现。
 */
@Service
public class OperationLogServiceImpl implements OperationLogService {

    private final SysOperationLogMapper sysOperationLogMapper;

    public OperationLogServiceImpl(SysOperationLogMapper sysOperationLogMapper) {
        this.sysOperationLogMapper = sysOperationLogMapper;
    }

    @Override
    public PageResult<OperationLogPageVO> pageQuery(OperationLogQueryDTO dto) {
        Page<SysOperationLog> page = new Page<>(dto.getCurrent(), dto.getSize());
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(dto.getModuleName()), SysOperationLog::getModuleName, dto.getModuleName())
                .like(StringUtils.hasText(dto.getOperatorName()), SysOperationLog::getOperatorName, dto.getOperatorName())
                .orderByDesc(SysOperationLog::getOperationTime);
        Page<SysOperationLog> result = sysOperationLogMapper.selectPage(page, wrapper);
        PageResult<OperationLogPageVO> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords().stream().map(item -> {
            OperationLogPageVO vo = new OperationLogPageVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).collect(Collectors.toList()));
        pageResult.setCurrent(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    @Override
    public void record(String moduleName, String operationType, String requestUri, String requestMethod,
                       Integer status, String errorMessage) {
        SysOperationLog log = new SysOperationLog();
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser != null) {
            log.setOperatorName(loginUser.getNickname());
            log.setOperatorUsername(loginUser.getUsername());
        }
        log.setModuleName(moduleName);
        log.setOperationType(operationType);
        log.setRequestUri(requestUri);
        log.setRequestMethod(requestMethod);
        log.setOperationStatus(status);
        log.setErrorMessage(errorMessage);
        log.setOperationTime(LocalDateTime.now());
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            log.setRequestParams(request.getQueryString());
        }
        sysOperationLogMapper.insert(log);
    }
}
