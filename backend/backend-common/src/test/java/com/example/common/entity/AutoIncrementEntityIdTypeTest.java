package com.example.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AutoIncrementEntityIdTypeTest {

    @Test
    void autoIncrementEntitiesShouldUseAutoIdType() throws NoSuchFieldException {
        assertAutoIdType(SysDept.class);
        assertAutoIdType(SysUser.class);
        assertAutoIdType(SysRole.class);
        assertAutoIdType(SysMenu.class);
        assertAutoIdType(SysUserRole.class);
        assertAutoIdType(SysRoleMenu.class);
        assertAutoIdType(SysLoginLog.class);
        assertAutoIdType(SysOperationLog.class);
    }

    private void assertAutoIdType(Class<?> entityClass) throws NoSuchFieldException {
        Field idField = entityClass.getDeclaredField("id");
        TableId tableId = idField.getAnnotation(TableId.class);
        assertNotNull(tableId, () -> entityClass.getSimpleName() + " should declare @TableId on id");
        assertEquals(IdType.AUTO, tableId.type(), () -> entityClass.getSimpleName() + " should use IdType.AUTO");
    }
}
