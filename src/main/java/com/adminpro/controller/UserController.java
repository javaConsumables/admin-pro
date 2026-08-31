package com.adminpro.controller;

import com.adminpro.annotation.NoRepeatSubmit;
import com.adminpro.annotation.OperationLog;
import com.adminpro.annotation.RequiresPermission;
import com.adminpro.common.Result;
import com.adminpro.common.exception.BusinessException;
import com.adminpro.dto.PasswordResetRequest;
import com.adminpro.dto.RoleIdsRequest;
import com.adminpro.dto.StatusUpdateRequest;
import com.adminpro.entity.SysUser;
import com.adminpro.entity.SysUserRole;
import com.adminpro.mapper.SysUserMapper;
import com.adminpro.mapper.SysUserRoleMapper;
import com.adminpro.service.UserService;
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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final UserService userService;

    @GetMapping("/page")
    @RequiresPermission("system:user:list")
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

    @GetMapping("/{id}")
    @RequiresPermission("system:user:list")
    public Result<SysUser> detail(@PathVariable Long id) throws Exception {
        return Result.success(userService.getByIdCached(id));
    }

    @PostMapping
    @RequiresPermission("system:user:add")
    @NoRepeatSubmit(intervalSeconds = 5)
    @OperationLog("新建用户")
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
    @RequiresPermission("system:user:edit")
    @OperationLog("修改用户状态")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(request.getStatus());
        userMapper.updateById(user);
        userService.evictCache(id);
        return Result.success();
    }

    @PutMapping("/{id}/password")
    @RequiresPermission("system:user:edit")
    @OperationLog("重置密码")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        String salt = PasswordUtil.generateSalt();
        user.setSalt(salt);
        user.setPassword(PasswordUtil.hash(request.getPassword(), salt));
        userMapper.updateById(user);
        userService.evictCache(id);
        return Result.success();
    }

    @PutMapping("/{id}/roles")
    @RequiresPermission("system:role:edit")
    @OperationLog("分配用户角色")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody RoleIdsRequest request) {
        if (userMapper.selectById(id) == null) {
            throw new BusinessException("用户不存在");
        }
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        for (Long roleId : request.getRoleIds()) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(id);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
        return Result.success();
    }
}
