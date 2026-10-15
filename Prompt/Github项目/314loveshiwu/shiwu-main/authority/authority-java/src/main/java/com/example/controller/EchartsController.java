package com.example.controller;/*
 * Copyright (c) 2023. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

import cn.hutool.db.Db;
import cn.hutool.db.ds.simple.SimpleDataSource;
import com.example.common.Result;
import com.example.service.MenuService;
import com.example.service.RoleService;
import com.example.service.UserService;
import com.sun.management.OperatingSystemMXBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.SQLException;
import java.util.*;

/**
 * @Description
 * @Author
 * @Data 2023/1/11 19:17
 */
@RestController
@RequestMapping("/echarts")
public class EchartsController {
    @Resource
    private UserService userService;
    @Resource
    private MenuService menuService;
    @Resource
    private RoleService roleService;
    @Value("${codegenerator.database}")
    private String database;
    @Value("${codegenerator.url}")
    private String url;
    @Value("${codegenerator.username}")
    private String username;
    @Value("${codegenerator.password}")
    private String password;
    /**
     * 获取系统参数
     *
     * @return
     */
    @GetMapping("/systemparameter")
    public Result systemparameter() throws SQLException {
        Map<String, List> map = new HashMap<>();
        String[] names={"用户总数", "菜单总数", "角色总数"};
        List<String>  ydata= Arrays.asList(names);
        List<Integer> xdata=new ArrayList<>();
//        获取用户总数
        xdata.add(userService.list().size());
//        获取菜单总数
        xdata.add(menuService.list().size());
//        获取角色总数
        xdata.add(roleService.list().size());

//        添加到map中
        map.put("series",xdata);
        map.put("yAxis",ydata);


        return Result.success(map);
    }
    @GetMapping("/queryinternalStorage")
    public Result queryinternalStorage(){
        OperatingSystemMXBean mem = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        float total=mem.getTotalPhysicalMemorySize()/ 1024 / 1024 ;
        float available=mem.getFreePhysicalMemorySize()/1024/1024;
        float occupy=total-available;
        return Result.success(occupy/total);
    }
    /**
     * 获取数据源
     *
     * @return
     */
    private DataSource getDatasource() {
        return new SimpleDataSource(url, username, password);
    }

}
