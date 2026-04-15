package com.example.common.redis;

import com.example.common.model.security.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LoginUserRedisSerializationTest {

    private final GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

    @Test
    void shouldDeserializeHistoricalLoginUserJsonContainingSuperAdminField() {
        String json = "{\"@class\":\"com.example.common.model.security.LoginUser\",\"userId\":1,\"username\":\"admin\",\"roles\":[\"java.util.Collections$SingletonList\",[\"SUPER_ADMIN\"]],\"permissions\":[\"java.util.Collections$EmptyList\",[]],\"superAdmin\":true}";

        LoginUser loginUser = (LoginUser) serializer.deserialize(json.getBytes(StandardCharsets.UTF_8));

        assertEquals(1L, loginUser.getUserId());
        assertEquals("admin", loginUser.getUsername());
        assertEquals(Collections.singletonList("SUPER_ADMIN"), loginUser.getRoles());
    }

    @Test
    void shouldNotSerializeDerivedSuperAdminPropertyIntoRedis() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setUsername("admin");
        loginUser.setRoles(Collections.singletonList("SUPER_ADMIN"));

        byte[] bytes = serializer.serialize(loginUser);
        String json = new String(bytes, StandardCharsets.UTF_8);

        assertFalse(json.contains("\"superAdmin\""));
    }
}
