package com.example.system.vo.dept;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门树节点。
 */
@Data
public class DeptTreeVO {

    private Long id;
    private Long parentId;
    private String deptName;
    private String leader;
    private String phone;
    private Integer sort;
    private Integer status;
    private String remark;
    private List<DeptTreeVO> children = new ArrayList<>();
}
