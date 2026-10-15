package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

import com.example.service.UserRoleService;
import com.example.entity.UserRole;


/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author   
 * @since 2023-01-09
 */
@RestController
@RequestMapping("/userRole")
    public class UserRoleController {

@Resource
private UserRoleService userRoleService;

@PostMapping
public Boolean save(@RequestBody UserRole userRole) {
        return userRoleService.saveOrUpdate(userRole);
        }

@DeleteMapping("/{id}")
public Boolean delete(@PathVariable Integer id) {
        return userRoleService.removeById(id);
        }

@GetMapping
public List<UserRole> findAll() {
        return userRoleService.list();
        }

@GetMapping("/{id}")
public List<UserRole> findOne(@PathVariable Integer id) {
        return userRoleService.list();
        }

@GetMapping("/page")
public Page<UserRole> findPage(@RequestParam Integer pageNum,
@RequestParam Integer pageSize) {
        return userRoleService.page(new Page<>(pageNum, pageSize));
        }

        }

