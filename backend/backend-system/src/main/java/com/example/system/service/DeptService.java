package com.example.system.service;

import com.example.system.dto.dept.DeptSaveDTO;
import com.example.system.vo.dept.DeptTreeVO;

import java.util.List;

/**
 * 部门管理服务。
 */
public interface DeptService {

    List<DeptTreeVO> treeList();

    void saveDept(DeptSaveDTO dto);

    void updateDept(Long id, DeptSaveDTO dto);

    void removeDept(Long id);
}
