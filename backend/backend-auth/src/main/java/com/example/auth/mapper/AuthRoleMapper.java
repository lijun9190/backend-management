package com.example.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色查询 Mapper。
 */
@Mapper
public interface AuthRoleMapper {

    @Select("select r.role_code from sys_role r inner join sys_user_role ur on r.id = ur.role_id " +
            "where ur.user_id = #{userId} and r.status = 1")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
