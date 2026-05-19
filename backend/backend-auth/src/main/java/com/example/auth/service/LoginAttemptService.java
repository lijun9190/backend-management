package com.example.auth.service;

import com.example.common.exception.BusinessException;
import com.example.common.redis.RedisOperator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 登录失败防爆破服务，按用户名和IP维度记录失败次数并执行临时锁定。
 */
@Service
public class LoginAttemptService {

    private static final String FAIL_PREFIX = "admin:login:fail:";
    private static final String LOCK_PREFIX = "admin:login:lock:";

    private final RedisOperator redisOperator;
    private final int maxFailures;
    private final long lockMinutes;
    private final long failureWindowMinutes;

    /**
     * 创建登录失败防爆破服务。
     *
     * @param redisOperator        Redis操作器
     * @param maxFailures          锁定前允许的最大失败次数
     * @param lockMinutes          锁定分钟数
     * @param failureWindowMinutes 失败计数窗口分钟数
     */
    public LoginAttemptService(RedisOperator redisOperator,
                               @Value("${security.login-attempt.max-failures:5}") int maxFailures,
                               @Value("${security.login-attempt.lock-minutes:15}") long lockMinutes,
                               @Value("${security.login-attempt.failure-window-minutes:15}") long failureWindowMinutes) {
        this.redisOperator = redisOperator;
        this.maxFailures = maxFailures;
        this.lockMinutes = lockMinutes;
        this.failureWindowMinutes = failureWindowMinutes;
    }

    /**
     * 检查当前用户名和IP是否处于锁定状态。
     *
     * @param username 用户名
     * @param ip       登录IP
     */
    public void checkAllowed(String username, String ip) {
        if (redisOperator.hasKey(lockKey(username, ip))) {
            throw new BusinessException("登录失败次数过多，请" + lockMinutes + "分钟后再试");
        }
    }

    /**
     * 记录一次登录失败，并在达到阈值时写入锁定标记。
     *
     * @param username 用户名
     * @param ip       登录IP
     */
    public void recordFailure(String username, String ip) {
        String failKey = failKey(username, ip);
        Integer currentFailures = redisOperator.get(failKey, Integer.class);
        int nextFailures = currentFailures == null ? 1 : currentFailures + 1;
        redisOperator.set(failKey, nextFailures, failureWindowMinutes, TimeUnit.MINUTES);
        if (nextFailures >= maxFailures) {
            redisOperator.set(lockKey(username, ip), "1", lockMinutes, TimeUnit.MINUTES);
        }
    }

    /**
     * 登录成功后清理失败计数和锁定标记。
     *
     * @param username 用户名
     * @param ip       登录IP
     */
    public void clear(String username, String ip) {
        redisOperator.delete(failKey(username, ip));
        redisOperator.delete(lockKey(username, ip));
    }

    private String failKey(String username, String ip) {
        return FAIL_PREFIX + normalize(username) + ":" + normalize(ip);
    }

    private String lockKey(String username, String ip) {
        return LOCK_PREFIX + normalize(username) + ":" + normalize(ip);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "unknown";
    }
}
