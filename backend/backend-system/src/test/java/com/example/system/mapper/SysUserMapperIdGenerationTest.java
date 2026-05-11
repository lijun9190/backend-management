package com.example.system.mapper;

import com.example.common.entity.SysUser;
import com.example.system.BackendSystemApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BackendSystemApplication.class)
class SysUserMapperIdGenerationTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    @Transactional
    void insertShouldUseDatabaseAutoIncrementId() {
        SysUser user = new SysUser();
        user.setDeptId(1L);
        user.setUsername("id-test-" + System.currentTimeMillis());
        user.setPassword("test");
        user.setNickname("ID Test");
        user.setRealName("ID Test");
        user.setStatus(1);
        user.setDeleted(0);
        user.setCreateBy("test");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateBy("test");
        user.setUpdateTime(LocalDateTime.now());

        int rows = sysUserMapper.insert(user);

        assertEquals(1, rows);
        assertNotNull(user.getId(), "insert should populate generated id back to entity");
        assertTrue(user.getId() > 0, "insert should use a positive database generated id");
    }
}
