package com.adminpro.aspect;

import com.adminpro.annotation.OperationLog;
import com.adminpro.component.OperationLogRecorder;
import com.adminpro.entity.SysOperationLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogRecorder recorder;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = true;
        String errorMsg = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            success = false;
            errorMsg = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
            throw t;
        } finally {
            try {
                SysOperationLog record = new SysOperationLog();
                ServletRequestAttributes attributes =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                HttpServletRequest request = attributes == null ? null : attributes.getRequest();
                if (request != null) {
                    Object userId = request.getAttribute("userId");
                    record.setUserId(userId instanceof Long l ? l : null);
                    Object username = request.getAttribute("username");
                    record.setUsername(username == null ? null : username.toString());
                    String ip = request.getHeader("X-Forwarded-For");
                    record.setIp(ip == null ? request.getRemoteAddr() : ip.split(",")[0].trim());
                }
                record.setOperation(operationLog.value());
                record.setMethod(((MethodSignature) joinPoint.getSignature()).getMethod().toString());
                record.setParams(buildParams(joinPoint.getArgs()));
                record.setCostTime(System.currentTimeMillis() - start);
                record.setStatus(success ? 1 : 0);
                record.setErrorMsg(errorMsg);
                recorder.recordAsync(record);
            } catch (Exception ignored) {
                // 日志记录失败不影响主流程
            }
        }
    }

    private String buildParams(Object[] args) {
        try {
            Object[] sanitized = Arrays.stream(args)
                    .map(a -> a instanceof MultipartFile ? "[file:" + ((MultipartFile) a).getOriginalFilename() + "]" : a)
                    .toArray();
            String json = objectMapper.writeValueAsString(sanitized);
            json = json.replaceAll("\"password\"\s*:\s*\"[^\"]*\"", "\"password\":\"******\"");
            return json.length() > 1000 ? json.substring(0, 1000) : json;
        } catch (Exception e) {
            return "[unserializable]";
        }
    }
}
