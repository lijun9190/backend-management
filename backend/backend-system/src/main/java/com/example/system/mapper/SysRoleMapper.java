package com.example.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色 Mapper。
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("select count(1) from sys_role where deleted = 0")
    Long countAllRoles();

    @Select("select role_id from sys_user_role where user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select("select distinct r.role_code from sys_role r inner join sys_user_role ur on r.id = ur.role_id where ur.user_id = #{userId} and r.deleted = 0")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
