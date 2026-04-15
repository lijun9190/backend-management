package com.example.system.service;

import com.example.common.model.result.PageResult;
import com.example.system.dto.log.LoginLogQueryDTO;
import com.example.system.vo.log.LoginLogPageVO;

/**
 * 登录日志服务。
 */
public interface LoginLogService {

    PageResult<LoginLogPageVO> pageQuery(LoginLogQueryDTO dto);
}
