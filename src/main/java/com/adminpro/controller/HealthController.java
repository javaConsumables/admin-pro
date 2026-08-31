package com.adminpro.controller;

import com.adminpro.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查 / 启动验证接口
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${spring.application.name}")
    private String appName;

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("app", appName);
        info.put("status", "UP");
        info.put("time", LocalDateTime.now().toString());
        return Result.success(info);
    }
}
