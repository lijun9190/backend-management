package com.example.system.vo.dashboard;

import com.example.system.vo.log.LoginLogPageVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 仪表盘总览数据。
 */
@Data
public class DashboardOverviewVO {

    private Long userCount;
    private Long roleCount;
    private Long menuCount;
    private Long recentLoginCount;
    private List<DashboardStatVO> cards = new ArrayList<>();
    private List<LoginLogPageVO> recentLoginLogs = new ArrayList<>();
}
