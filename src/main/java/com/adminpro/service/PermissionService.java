package com.adminpro.service;

import com.adminpro.common.exception.BusinessException;
import com.adminpro.common.ResultCode;
import com.adminpro.entity.SysMenu;
import com.adminpro.entity.SysRole;
import com.adminpro.entity.SysRoleMenu;
import com.adminpro.entity.SysUserRole;
import com.adminpro.mapper.SysMenuMapper;
import com.adminpro.mapper.SysRoleMapper;
import com.adminpro.mapper.SysRoleMenuMapper;
import com.adminpro.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限查询：admin 角色放行全部，其余按 用户-角色-权限 三级查询
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    /** 用户是否为超管 */
    public boolean isAdmin(Long userId) {
        List<SysRole> roles = getUserRoles(userId);
        return roles.stream().anyMatch(r -> "admin".equals(r.getRoleCode()));
    }

    public List<SysRole> getUserRoles(Long userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        return roleMapper.selectBatchIds(roleIds);
    }

    /** 用户拥有的权限标识集合 */
    public Set<String> getUserPerms(Long userId) {
        if (isAdmin(userId)) {
            return menuMapper.selectList(null).stream()
                    .map(SysMenu::getPerms)
                    .filter(p -> p != null && !p.isBlank())
                    .collect(Collectors.toSet());
        }
        List<SysRole> roles = getUserRoles(userId);
        if (roles.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
        if (roleMenus.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
        return menuMapper.selectBatchIds(menuIds).stream()
                .map(SysMenu::getPerms)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.toSet());
    }

    public void checkPermission(Long userId, String perm) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (isAdmin(userId)) {
            return;
        }
        if (!getUserPerms(userId).contains(perm)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
