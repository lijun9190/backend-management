package com.example.system.mapper;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.example.common.entity.SysUser;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 用户ID生成策略测试，避免依赖真实数据库即可验证实体映射约束。
 */
class SysUserMapperIdGenerationTest {

    /**
     * 验证用户主键声明为数据库自增，防止误改为雪花ID等策略。
     */
    @Test
    void userIdShouldUseDatabaseAutoIncrementStrategy() throws Exception {
        Field idField = SysUser.class.getDeclaredField("id");
        TableId tableId = idField.getAnnotation(TableId.class);

        assertNotNull(tableId);
        assertEquals(IdType.AUTO, tableId.type());
    }
}
