package com.adminpro.controller;

import com.adminpro.annotation.RequiresPermission;
import com.adminpro.common.Result;
import com.adminpro.entity.SysOperationLog;
import com.adminpro.mapper.SysOperationLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询（Day 7-8）
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final SysOperationLogMapper logMapper;

    @GetMapping("/page")
    @RequiresPermission("system:log:list")
    public Result<Page<SysOperationLog>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) String operation) {
        return Result.success(logMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysOperationLog>()
                        .like(StringUtils.hasText(operation), SysOperationLog::getOperation, operation)
                        .orderByDesc(SysOperationLog::getId)));
    }
}
