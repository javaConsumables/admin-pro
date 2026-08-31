package com.adminpro.controller;

import com.adminpro.common.Result;
import com.adminpro.common.exception.BusinessException;
import com.adminpro.dto.PasswordResetRequest;
import com.adminpro.dto.StatusUpdateRequest;
import com.adminpro.entity.SysUser;
import com.adminpro.mapper.SysUserMapper;
import com.adminpro.util.PasswordUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 用户管理（Day 3-4）
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserMapper userMapper;

    @GetMapping("/page")
    public Result<Page<SysUser>> page(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(username), SysUser::getUsername, username)
                .orderByDesc(SysUser::getId)
                .select(SysUser::getId, SysUser::getUsername, SysUser::getNickname,
                        SysUser::getEmail, SysUser::getPhone, SysUser::getStatus,
                        SysUser::getCreateTime, SysUser::getUpdateTime);
        return Result.success(userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @PostMapping
    public Result<SysUser> create(@Valid @RequestBody SysUser user) {
        if (!StringUtils.hasText(user.getUsername())) {
            throw new BusinessException("用户名不能为空");
        }
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException("用户名已存在");
        }
        String salt = PasswordUtil.generateSalt();
        user.setSalt(salt);
        user.setPassword(PasswordUtil.hash(user.getPassword() == null ? "123456" : user.getPassword(), salt));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userMapper.insert(user);
        user.setPassword(null);
        user.setSalt(null);
        return Result.success(user);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(request.getStatus());
        userMapper.updateById(user);
        return Result.success();
    }

    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        String salt = PasswordUtil.generateSalt();
        user.setSalt(salt);
        user.setPassword(PasswordUtil.hash(request.getPassword(), salt));
        userMapper.updateById(user);
        return Result.success();
    }
}
