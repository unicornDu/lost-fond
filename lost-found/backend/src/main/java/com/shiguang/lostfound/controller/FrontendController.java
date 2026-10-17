package com.shiguang.lostfound.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * P3：前端 SPA 回退——托管模式（/** → dist）下，history 路由的直接访问/刷新
 * （/home、/wall、/item/1 …）没有对应实体文件会 404，这里转发回 index.html
 * 交给前端路由。路由清单与 frontend/src/router/index.js 保持一致（新增页面同步补）。
 * /api、/uploads、/file 均为独立控制器/映射，不受影响。
 */
@Controller
public class FrontendController {

    @GetMapping({"/", "/home", "/wall", "/publish", "/search-image", "/profile", "/auth", "/login",
            "/item/*", "/user/*", "/admin", "/admin/*"})
    public String spa() {
        return "forward:/index.html";
    }
}
