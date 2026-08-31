package com.adminpro.controller;

import com.adminpro.annotation.OperationLog;
import com.adminpro.annotation.RequiresPermission;
import com.adminpro.common.Result;
import com.adminpro.common.exception.BusinessException;
import com.adminpro.entity.SysMenu;
import com.adminpro.mapper.SysMenuMapper;
import com.adminpro.vo.MenuTreeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final SysMenuMapper menuMapper;

    @GetMapping("/tree")
    @RequiresPermission("system:menu:list")
    public Result<List<MenuTreeVO>> tree() {
        List<SysMenu> menus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort, SysMenu::getId));
        Map<Long, List<SysMenu>> byParent = menus.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        List<MenuTreeVO> roots = new ArrayList<>();
        for (SysMenu menu : byParent.getOrDefault(0L, List.of())) {
            roots.add(build(menu, byParent));
        }
        return Result.success(roots);
    }

    private MenuTreeVO build(SysMenu menu, Map<Long, List<SysMenu>> byParent) {
        MenuTreeVO vo = new MenuTreeVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setMenuName(menu.getMenuName());
        vo.setPerms(menu.getPerms());
        vo.setMenuType(menu.getMenuType());
        vo.setPath(menu.getPath());
        vo.setSort(menu.getSort());
        vo.setStatus(menu.getStatus());
        for (SysMenu child : byParent.getOrDefault(menu.getId(), List.of())) {
            vo.getChildren().add(build(child, byParent));
        }
        return vo;
    }

    @PostMapping
    @RequiresPermission("system:menu:add")
    @OperationLog("新增菜单权限")
    public Result<SysMenu> create(@RequestBody SysMenu menu) {
        menu.setId(null);
        menuMapper.insert(menu);
        return Result.success(menu);
    }

    @PutMapping("/{id}")
    @RequiresPermission("system:menu:edit")
    @OperationLog("修改菜单权限")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysMenu menu) {
        if (menuMapper.selectById(id) == null) {
            throw new BusinessException("菜单不存在");
        }
        menu.setId(id);
        menuMapper.updateById(menu);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("system:menu:delete")
    @OperationLog("删除菜单权限")
    public Result<Void> delete(@PathVariable Long id) {
        Long childCount = menuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException("存在子菜单，无法删除");
        }
        menuMapper.deleteById(id);
        return Result.success();
    }
}
