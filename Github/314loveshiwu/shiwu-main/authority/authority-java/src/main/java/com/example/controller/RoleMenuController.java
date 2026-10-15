package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

import com.example.service.RoleMenuService;
import com.example.entity.RoleMenu;


/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author   
 * @since 2023-01-09
 */
@RestController
@RequestMapping("/roleMenu")
    public class RoleMenuController {

@Resource
private RoleMenuService roleMenuService;

@PostMapping
public Boolean save(@RequestBody RoleMenu roleMenu) {
        return roleMenuService.saveOrUpdate(roleMenu);
        }

@DeleteMapping("/{id}")
public Boolean delete(@PathVariable Integer id) {
        return roleMenuService.removeById(id);
        }

@GetMapping
public List<RoleMenu> findAll() {
        return roleMenuService.list();
        }

@GetMapping("/{id}")
public List<RoleMenu> findOne(@PathVariable Integer id) {
        return roleMenuService.list();
        }

@GetMapping("/page")
public Page<RoleMenu> findPage(@RequestParam Integer pageNum,
@RequestParam Integer pageSize) {
        return roleMenuService.page(new Page<>(pageNum, pageSize));
        }

        }

