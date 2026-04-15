package com.example.auth.mapper;

import com.example.common.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单与权限查询 Mapper。
 */
@Mapper
public interface AuthMenuMapper {

    @Select("select distinct m.permission_code from sys_menu m " +
            "inner join sys_role_menu rm on m.id = rm.menu_id " +
            "inner join sys_user_role ur on rm.role_id = ur.role_id " +
            "where ur.user_id = #{userId} and m.status = 1 and m.permission_code is not null and m.permission_code != ''")
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    @Select("select distinct m.* from sys_menu m " +
            "inner join sys_role_menu rm on m.id = rm.menu_id " +
            "inner join sys_user_role ur on rm.role_id = ur.role_id " +
            "where ur.user_id = #{userId} and m.status = 1 order by m.sort asc, m.id asc")
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

    @Select("select * from sys_menu where status = 1 order by sort asc, id asc")
    List<SysMenu> selectAllMenus();
}
