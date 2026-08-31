package com.adminpro.aspect;

import com.adminpro.annotation.NoRepeatSubmit;
import com.adminpro.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交：Redisson 分布式锁（RLock + tryLock 带过期时间）
 */
@Aspect
@Component
@RequiredArgsConstructor
public class NoRepeatSubmitAspect {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Around("@annotation(noRepeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, NoRepeatSubmit noRepeatSubmit) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();
        Long userId = request == null ? null : (Long) request.getAttribute("userId");
        String uri = request == null ? "" : request.getRequestURI();
        String paramsHash = sha256(objectMapper.writeValueAsString(joinPoint.getArgs()));
        String lockKey = "adminpro:norepeat:" + userId + ":" + uri + ":" + paramsHash;

        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired;
        try {
            acquired = lock.tryLock(0, noRepeatSubmit.intervalSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("请求处理被中断");
        }
        if (!acquired) {
            throw new BusinessException("请勿重复提交，请稍后再试");
        }
        try {
            return joinPoint.proceed();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String sha256(String text) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest(text.getBytes(StandardCharsets.UTF_8)));
    }
}
