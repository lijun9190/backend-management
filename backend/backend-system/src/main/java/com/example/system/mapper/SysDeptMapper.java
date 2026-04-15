package com.example.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 部门 Mapper。
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    @Select("select count(1) from sys_dept where parent_id = #{parentId} and deleted = 0")
    Long countByParentId(@Param("parentId") Long parentId);
}
