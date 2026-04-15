package com.example.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.context.LoginUserContext;
import com.example.common.entity.SysDept;
import com.example.common.exception.BusinessException;
import com.example.common.model.security.LoginUser;
import com.example.system.dto.dept.DeptSaveDTO;
import com.example.system.mapper.SysDeptMapper;
import com.example.system.service.DeptService;
import com.example.system.service.OperationLogService;
import com.example.system.vo.dept.DeptTreeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门管理服务实现。
 */
@Service
public class DeptServiceImpl implements DeptService {

    private final SysDeptMapper sysDeptMapper;
    private final OperationLogService operationLogService;

    public DeptServiceImpl(SysDeptMapper sysDeptMapper, OperationLogService operationLogService) {
        this.sysDeptMapper = sysDeptMapper;
        this.operationLogService = operationLogService;
    }

    @Override
    public List<DeptTreeVO> treeList() {
        List<SysDept> depts = sysDeptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeleted, 0)
                .orderByAsc(SysDept::getSort)
                .orderByAsc(SysDept::getId));
        return buildTree(depts, 0L);
    }

    @Override
    public void saveDept(DeptSaveDTO dto) {
        SysDept dept = new SysDept();
        BeanUtils.copyProperties(dto, dept);
        fillAudit(dept, true);
        dept.setDeleted(0);
        sysDeptMapper.insert(dept);
        operationLogService.record("部门管理", "新增", "/api/system/depts", "POST", 1, null);
    }

    @Override
    public void updateDept(Long id, DeptSaveDTO dto) {
        SysDept dept = sysDeptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        BeanUtils.copyProperties(dto, dept);
        fillAudit(dept, false);
        sysDeptMapper.updateById(dept);
        operationLogService.record("部门管理", "编辑", "/api/system/depts/" + id, "PUT", 1, null);
    }

    @Override
    public void removeDept(Long id) {
        if (sysDeptMapper.countByParentId(id) > 0) {
            throw new BusinessException("当前部门下存在子部门，无法删除");
        }
        SysDept dept = sysDeptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        dept.setDeleted(1);
        fillAudit(dept, false);
        sysDeptMapper.updateById(dept);
        operationLogService.record("部门管理", "删除", "/api/system/depts/" + id, "DELETE", 1, null);
    }

    private List<DeptTreeVO> buildTree(List<SysDept> depts, Long parentId) {
        List<DeptTreeVO> list = new ArrayList<>();
        List<SysDept> current = depts.stream()
                .filter(item -> (item.getParentId() == null ? 0L : item.getParentId()) ==(parentId))
                .sorted(Comparator.comparing(SysDept::getSort, Comparator.nullsLast(Integer::compareTo)).thenComparing(SysDept::getId))
                .collect(Collectors.toList());
        for (SysDept dept : current) {
            DeptTreeVO vo = new DeptTreeVO();
            BeanUtils.copyProperties(dept, vo);
            vo.setChildren(buildTree(depts, dept.getId()));
            list.add(vo);
        }
        return list;
    }

    private void fillAudit(SysDept dept, boolean insert) {
        LoginUser loginUser = LoginUserContext.get();
        String username = loginUser == null ? "system" : loginUser.getUsername();
        if (insert) {
            dept.setCreateBy(username);
            dept.setCreateTime(LocalDateTime.now());
        }
        dept.setUpdateBy(username);
        dept.setUpdateTime(LocalDateTime.now());
    }
}
