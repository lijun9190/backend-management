package com.example.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.entity.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 登录日志 Mapper。
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {

    @Select("select count(1) from sys_login_log where login_time >= DATE_SUB(now(), interval 7 day)")
    Long countRecentSevenDays();

    @Select("select * from sys_login_log order by login_time desc limit 8")
    List<SysLoginLog> selectRecentList();
}
