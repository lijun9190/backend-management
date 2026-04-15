package com.example.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单 Mapper。
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("select count(1) from sys_menu where deleted = 0")
    Long countAllMenus();

    @Select("select menu_id from sys_role_menu where role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    @Select("select * from sys_menu where deleted = 0 order by sort asc, id asc")
    List<SysMenu> selectAllMenus();

    @Select("select count(1) from sys_menu where parent_id = #{parentId} and deleted = 0")
    Long countByParentId(@Param("parentId") Long parentId);
}
