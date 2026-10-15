package com.example.controller;


import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.Const;
import com.example.common.Constant;
import com.example.common.QueryPageParam;
import com.example.common.Result;
import com.example.entity.Goods;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.entity.UserRole;
import com.example.service.RoleService;
import com.example.service.UserRoleService;
import com.example.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 作者, 版本1.1.0!微信号：navicat15
 * @since 2023-01-07
 */

@RestController
@RequestMapping("/sys/user")
public class UserController {
    @Resource
    UserService userService;
    @Resource
    UserRoleService userRoleService;
    @Resource
    private RoleService roleService;

    /**
     * 获取个人信息
     *
     * @param principal
     * @return
     */
    @GetMapping("/getuserInfo")
    public Result getuserInfo(Principal principal) {
        User user = userService.getByUsername(principal.getName());
        return Result.success(user);
    }

    /**
     * 分页查询
     *
     * @return
     */
    @GetMapping("/page")
    public Result page(
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Page<User> page = userService.page(new Page<>(pageNum, pageSize), new QueryWrapper<User>().like("username", username));
        List<User> list = page.getRecords();
        for (User user : list) {
            QueryWrapper<UserRole> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", user.getId());

//        先获取所有的uroles
            List<UserRole> uroles = userRoleService.list(queryWrapper);
            List<Long> ur = new ArrayList<>();
            if (uroles.size() > 0) {
                for (UserRole userRole : uroles) {
                    ur.add(userRole.getRoleId());
                }
                List<Role> roles = roleService.listByIds(ur);
                user.setSysRoles(roles);
            }


        }
        page.setRecords(list);
        return Result.success(page);
    }

    /**
     * 密码加密保存
     *
     * @return
     */
    @Resource
    BCryptPasswordEncoder passwordEncoder;

    /**
     * 保存
     *
     * @param user
     * @return
     */

    @PostMapping("/save")
    public Result save(@RequestBody User user) {
        user.setCreated(LocalDateTime.now());
        user.setStatu(Const.STATUS_OFF);
        user.setPassword(passwordEncoder.encode(Const.DEFULT_PASSWORD));
        user.setAvatar(Const.DEFULT_AVATAR);
        userService.save(user);
        return Result.success("");
    }

    /**
     * 修改数据
     *
     * @param user
     * @return
     */
    @PostMapping("/update")
    public Result update(@RequestBody User user) {
        user.setUpdated(LocalDateTime.now());
        userService.saveOrUpdate(user);

        return Result.success(user);
    }

    /**
     * 获取用户和角色信息
     *
     * @return
     */
    @PostMapping("/info/{id}")
    @Transactional
    public Result info(@PathVariable long id) {
        User user = userService.getById(id);
        Assert.notNull(user, "用户不存在");
        List<Role> roles = roleService.listByUserId(id);
        user.setRoles(roles);
        return Result.success(user);
    }


    @PostMapping("/delete")
    @Transactional
    public Result delete(@RequestBody Long[] ids) {
        List<Long> userIds = Arrays.asList(ids);
        return Result.success(userService.removeByIds(userIds));
    }

    /**
     * 重置密码
     *
     * @return
     */
    @PostMapping("/repass/{id}")
    public Result repass(@PathVariable Long id) {
        User user = userService.getById(id);
        user.setPassword(passwordEncoder.encode(Const.DEFULT_PASSWORD));
        userService.saveOrUpdate(user);
        return Result.success("");
    }

    /**
     * 校验密码是否一致
     *
     * @param user
     * @return
     */
    @PostMapping("/checkpass")
    public Result checkpass(@RequestBody User user) {

        User sysUser = userService.getById(user.getId());
        boolean matches = passwordEncoder.matches(user.getPassword(), sysUser.getPassword()); //true
        if (matches) {
            return Result.success("ok");
        } else {
            return Result.success("error");
        }

    }

    /**
     * 修改密码
     *
     * @param user
     * @return
     */
    @PostMapping("/updatepass")
    public Result updatepass(@RequestBody User user) {

        User sysUser = userService.getById(user.getId());
        sysUser.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.saveOrUpdate(sysUser);
        return Result.success("ok");
    }

    /**
     * 注册模块
     *
     * @param user
     * @return
     */
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        user.setStatu(0);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setAvatar(Const.DEFULT_AVATAR);
        QueryWrapper<User> queryWrapper = new QueryWrapper();
        queryWrapper.eq("username", user.getUsername());
        if (userService.list(queryWrapper).size() > 0) {
            return Result.error("账户名已经存在请更换");
        } else {
            userService.save(user);
            user = userService.getByUsername(user.getUsername());
//           设置默认角色
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(Constant.DEFUALT_ROLE);
            userRoleService.save(userRole);
            return Result.success("");
        }

    }


}
