package com.adminpro.controller;

import com.adminpro.annotation.OperationLog;
import com.adminpro.annotation.RequiresPermission;
import com.adminpro.common.Result;
import com.adminpro.common.exception.BusinessException;
import com.adminpro.dto.MenuIdsRequest;
import com.adminpro.entity.SysRole;
import com.adminpro.entity.SysRoleMenu;
import com.adminpro.mapper.SysRoleMapper;
import com.adminpro.mapper.SysRoleMenuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @GetMapping("/page")
    @RequiresPermission("system:role:list")
    public Result<Page<SysRole>> page(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) String roleName) {
        return Result.success(roleMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysRole>()
                        .like(StringUtils.hasText(roleName), SysRole::getRoleName, roleName)
                        .orderByDesc(SysRole::getId)));
    }

    @GetMapping("/all")
    @RequiresPermission("system:role:list")
    public Result<List<SysRole>> all() {
        return Result.success(roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getStatus, 1).orderByAsc(SysRole::getId)));
    }

    @PostMapping
    @RequiresPermission("system:role:add")
    @OperationLog("新增角色")
    public Result<SysRole> create(@Valid @RequestBody SysRole role) {
        if (!StringUtils.hasText(role.getRoleName()) || !StringUtils.hasText(role.getRoleCode())) {
            throw new BusinessException("角色名称和编码不能为空");
        }
        Long count = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, role.getRoleCode()));
        if (count != null && count > 0) {
            throw new BusinessException("角色编码已存在");
        }
        role.setId(null);
        roleMapper.insert(role);
        return Result.success(role);
    }

    @PutMapping("/{id}")
    @RequiresPermission("system:role:edit")
    @OperationLog("修改角色")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysRole role) {
        SysRole db = roleMapper.selectById(id);
        if (db == null) {
            throw new BusinessException("角色不存在");
        }
        if ("admin".equals(db.getRoleCode())) {
            throw new BusinessException("内置管理员角色不可修改");
        }
        role.setId(id);
        roleMapper.updateById(role);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("system:role:delete")
    @OperationLog("删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        SysRole db = roleMapper.selectById(id);
        if (db == null) {
            throw new BusinessException("角色不存在");
        }
        if ("admin".equals(db.getRoleCode())) {
            throw new BusinessException("内置管理员角色不可删除");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        return Result.success();
    }

    @PutMapping("/{id}/menus")
    @RequiresPermission("system:role:edit")
    @OperationLog("分配角色权限")
    public Result<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody MenuIdsRequest request) {
        if (roleMapper.selectById(id) == null) {
            throw new BusinessException("角色不存在");
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        for (Long menuId : request.getMenuIds()) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(id);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
        return Result.success();
    }
}
