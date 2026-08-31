package com.adminpro.component;

import com.adminpro.entity.SysOperationLog;
import com.adminpro.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogRecorder {

    private final SysOperationLogMapper logMapper;

    @Async("operationLogExecutor")
    public void recordAsync(SysOperationLog record) {
        try {
            logMapper.insert(record);
        } catch (Exception e) {
            log.error("操作日志写入失败: {}", e.getMessage());
        }
    }
}
