package com.example.controller;/*
 * Copyright (c) 2023. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.Result;
import com.example.entity.Role;
import com.example.entity.RoleMenu;
import com.example.entity.UserRole;
import com.example.service.RoleMenuService;
import com.example.service.RoleService;
import com.example.service.UserRoleService;
import com.example.service.UserService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author
 * @Data 2023/1/10 17:20
 */
@RestController
@RequestMapping("/sys/role")
public class RoleController {

    @Resource
    private RoleService roleService;
    @Resource
    private UserService userService;
    @Resource
    private RoleMenuService roleMenuService;
    @Resource
    UserRoleService userRoleService;

    /**
     * 获取用户相关联的菜单
     *
     * @param id
     * @return
     */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long id) {
        Role role = roleService.getById(id);
        List<RoleMenu> roleMenus = roleMenuService.list(new QueryWrapper<RoleMenu>().eq("role_id", id));
        List<Long> menuIds = roleMenus.stream().map(p -> p.getMenuId()).collect(Collectors.toList());
        role.setMenuIds(menuIds);
        return Result.success(role);
    }


    /**
     * 分页
     *
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public Result findPage(@RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        QueryWrapper<Role> queryWrapper=new QueryWrapper<>();
        if(!"".equals(name)){
            queryWrapper.like("name",name);
        }

        return Result.success(roleService.page(new Page<>(pageNum, pageSize),queryWrapper));
    }

    /**
     * 添加角色
     *
     * @param role
     * @return
     */
    @PostMapping("/save")
    public Result save(@RequestBody Role role) {
        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", role.getName()).eq("code", role.getCode());
        if (roleService.list(queryWrapper).size() > 0) {
            return Result.error("不可重复添加相同的数据");

        } else {
            role.setCreated(LocalDateTime.now());
            return Result.success(roleService.save(role));
        }
    }

    /**
     * 修改角色
     *
     * @param role
     * @return
     */
    @PostMapping("/update")
    public Result update(@RequestBody Role role) {
        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", role.getName()).eq("code", role.getCode());
        if (roleService.list(queryWrapper).size() > 1) {
            return Result.error("不可重复添加相同的数据");

        } else {
            role.setUpdated(LocalDateTime.now());
            userService.clearUserAuthorityByRole(role.getId());
            return Result.success(roleService.saveOrUpdate(role));
        }
    }

    /**
     * 删除角色
     *
     * @return
     */
    @PostMapping("/delete")
    @Transactional
    public Result Delete(@RequestBody Long[] ids) {
        /**
         * 删除中间表
         */
        userRoleService.remove(new QueryWrapper<UserRole>().in("role_id", ids));
        roleMenuService.remove(new QueryWrapper<RoleMenu>().in("role_id", ids));
        Arrays.stream(ids).forEach(id -> {
            userService.clearUserAuthorityByRole(id);

        });
        roleService.removeByIds(Arrays.asList(ids));
        return Result.success("ok");
    }

    /**
     * 展示权限
     *
     * @param roleId
     * @return
     */
    @PostMapping("/perm/{roleId}")
    @Transactional
    public Result perm(@PathVariable("roleId") Long roleId, @RequestBody long[] menuIds) {
        List<RoleMenu> roleMenus = new ArrayList<>();

        Arrays.stream(menuIds).forEach(menuId -> {
            RoleMenu roleMenu = new RoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            roleMenus.add(roleMenu);
        });
        roleMenuService.remove(new QueryWrapper<RoleMenu>().eq("role_id", roleId));
        roleMenuService.saveBatch(roleMenus);
        userService.clearUserAuthorityByRole(roleId);

        return Result.success(menuIds);
    }
}
