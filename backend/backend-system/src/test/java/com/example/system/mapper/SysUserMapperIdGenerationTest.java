package com.example.system.mapper;

import com.example.common.entity.SysUser;
import com.example.system.BackendSystemApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = BackendSystemApplication.class)
class SysUserMapperIdGenerationTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void insertShouldUseDatabaseAutoIncrementId() {
        Long expectedNextId = jdbcTemplate.queryForObject(
                "SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user'",
                Long.class
        );

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
        assertEquals(expectedNextId, user.getId(), "insert should use database auto increment instead of application generated id");
    }
}
