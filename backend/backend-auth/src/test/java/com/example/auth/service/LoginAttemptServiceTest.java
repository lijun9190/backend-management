package com.example.auth.service;

import com.example.common.exception.BusinessException;
import com.example.common.redis.RedisOperator;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录失败防爆破测试，验证失败计数、锁定和成功清理逻辑。
 */
class LoginAttemptServiceTest {

    /**
     * 达到最大失败次数时应写入锁定标记。
     */
    @Test
    void recordFailureShouldLockUsernameAndIpWhenThresholdReached() {
        RedisOperator redisOperator = mock(RedisOperator.class);
        LoginAttemptService service = new LoginAttemptService(redisOperator, 5, 15, 15);
        when(redisOperator.get("admin:login:fail:admin:127.0.0.1", Integer.class)).thenReturn(4);

        service.recordFailure("admin", "127.0.0.1");

        verify(redisOperator).set("admin:login:fail:admin:127.0.0.1", 5, 15L, TimeUnit.MINUTES);
        verify(redisOperator).set("admin:login:lock:admin:127.0.0.1", "1", 15L, TimeUnit.MINUTES);
    }

    /**
     * 锁定期间应直接拒绝登录。
     */
    @Test
    void checkAllowedShouldRejectLockedUsernameAndIp() {
        RedisOperator redisOperator = mock(RedisOperator.class);
        LoginAttemptService service = new LoginAttemptService(redisOperator, 5, 15, 15);
        when(redisOperator.hasKey("admin:login:lock:admin:127.0.0.1")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.checkAllowed("admin", "127.0.0.1"));

        assertEquals("登录失败次数过多，请15分钟后再试", exception.getMessage());
    }

    /**
     * 登录成功后应清理失败计数和锁定标记。
     */
    @Test
    void clearShouldDeleteFailureAndLockKeys() {
        RedisOperator redisOperator = mock(RedisOperator.class);
        LoginAttemptService service = new LoginAttemptService(redisOperator, 5, 15, 15);

        service.clear("admin", "127.0.0.1");

        verify(redisOperator).delete(eq("admin:login:fail:admin:127.0.0.1"));
        verify(redisOperator).delete(eq("admin:login:lock:admin:127.0.0.1"));
    }
}
