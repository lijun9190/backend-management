package com.example.common.aspect;

import com.example.common.annotation.OperLog;
import com.example.common.context.LoginUserContext;
import com.example.common.model.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 操作日志切面。
 *
 * 这里先以应用日志形式输出，system 模块会在此基础上真正落库。
 */
@Slf4j
@Aspect
@Component
public class OperLogAspect {

    @Around("@annotation(com.example.common.annotation.OperLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        OperLog operLog = method.getAnnotation(OperLog.class);
        LoginUser loginUser = LoginUserContext.get();
        try {
            Object result = joinPoint.proceed();
            log.info("操作日志-成功 module={}, type={}, operator={}, cost={}ms",
                    operLog.module(),
                    operLog.type(),
                    loginUser == null ? "anonymous" : loginUser.getUsername(),
                    System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            log.error("操作日志-失败 module={}, type={}, operator={}, cost={}ms",
                    operLog.module(),
                    operLog.type(),
                    loginUser == null ? "anonymous" : loginUser.getUsername(),
                    System.currentTimeMillis() - start,
                    ex);
            throw ex;
        }
    }
}
