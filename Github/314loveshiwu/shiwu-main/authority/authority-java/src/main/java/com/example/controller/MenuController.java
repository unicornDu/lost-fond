package com.example.controller;


import cn.hutool.core.map.MapUtil;
import com.example.common.Result;
import com.example.controller.dto.MenuDto;
import com.example.entity.Menu;
import com.example.entity.User;
import com.example.service.MenuService;
import com.example.service.UserService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;


/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author authority
 * @since 2023-01-03
 */
@RestController

@RequestMapping("/sys/menu")
public class MenuController {

    @Resource
    private UserService userService;
    @Resource
    private MenuService menuService;

    @GetMapping("/nav")
    public Result getNavs(Principal principal) {

        User user = userService.getByUsername(principal.getName());

//          获取权限信息

        String authorityInfo = userService.getUserAuthorityInfo(user.getId());

//        字符串转换成数组
        String[] tokenizeToStringArray = StringUtils.tokenizeToStringArray(authorityInfo, ",");
//
////        获取导航栏信息
        List<MenuDto> menuDtoList = menuService.getCurrentUserNavs();

        return Result.success(MapUtil.builder().
                put("authoritys", tokenizeToStringArray).
                put("nav", menuDtoList).map());

    }

    /**
     * 获取菜单数据信息
     *
     * @param
     * @return
     */
    @GetMapping("/list")
    public Result getmenuList() {
        return Result.success(menuService.getTree());
    }

    /**
     * 修改菜单信息
     *
     * @param id
     * @return
     */
    @GetMapping("/info/{id}")
    public Result getInfo(@PathVariable(name = "id") Long id) {
        return Result.success(menuService.getById(id));
    }

    @PostMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        return Result.success(menuService.removeById(id));
    }

    /**
     * 保存或者修改
     *
     * @param menu
     * @return
     */
    @PostMapping("/save")
    public Result save(@RequestBody Menu menu) {
        if (menu.getId()==null) {
            menu.setCreated(LocalDateTime.now());
        } else {
            menu.setUpdated(LocalDateTime.now());
        }
        return Result.success(menuService.saveOrUpdate(menu));
    }
}
