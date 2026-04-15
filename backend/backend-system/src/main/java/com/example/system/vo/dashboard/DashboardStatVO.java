package com.example.system.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仪表盘统计卡片。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatVO {

    private String label;
    private Long value;
}
