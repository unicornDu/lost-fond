package com.shiguang.lostfound.controller;

import com.shiguang.lostfound.common.PageResult;
import com.shiguang.lostfound.common.Result;
import com.shiguang.lostfound.context.UserContext;
import com.shiguang.lostfound.entity.AdminLog;
import com.shiguang.lostfound.service.AdminService;
import com.shiguang.lostfound.service.StatsService;
import com.shiguang.lostfound.vo.ItemVO;
import com.shiguang.lostfound.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * M6 管理后台：用户管理、信息审核、违规下架、统计看板。所有接口需管理员身份。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final StatsService statsService;

    @GetMapping("/users")
    public Result<PageResult<UserVO>> users(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword) {
        UserContext.requireAdmin();
        return Result.ok(adminService.listUsers(page, size, keyword));
    }

    @PostMapping("/users/{id}/status")
    public Result<Void> setUserStatus(@PathVariable Long id, @RequestParam boolean ban) {
        Long adminId = UserContext.requireAdmin();
        adminService.setUserStatus(adminId, id, ban);
        return Result.ok();
    }

    @GetMapping("/items")
    public Result<PageResult<ItemVO>> items(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) Integer auditStatus,
                                            @RequestParam(required = false) String type) {
        UserContext.requireAdmin();
        return Result.ok(adminService.listItems(page, size, auditStatus, type));
    }

    @PostMapping("/items/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestParam boolean approve) {
        Long adminId = UserContext.requireAdmin();
        adminService.auditItem(adminId, id, approve);
        return Result.ok();
    }

    @PostMapping("/items/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        Long adminId = UserContext.requireAdmin();
        adminService.offlineItem(adminId, id);
        return Result.ok();
    }

    @DeleteMapping("/items/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long adminId = UserContext.requireAdmin();
        adminService.deleteItem(adminId, id);
        return Result.ok();
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        UserContext.requireAdmin();
        return Result.ok(statsService.dashboard());
    }

    /** 05 A-01：六表行数（仅管理员），供 test_api.py 幂等自检与运维核对。 */
    @GetMapping("/db-counts")
    public Result<Map<String, Long>> dbCounts() {
        UserContext.requireAdmin();
        return Result.ok(adminService.dbCounts());
    }

    /** 审查 2.29③：操作日志分页（admin_log 只写不读的问题）。 */
    @GetMapping("/logs")
    public Result<PageResult<AdminLog>> logs(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "15") long size) {
        UserContext.requireAdmin();
        return Result.ok(adminService.listLogs(page, size));
    }

    /** 审查 2.17：为历史图片补生成缩略图。 */
    @PostMapping("/regen-thumbs")
    public Result<Map<String, Object>> regenThumbs() {
        Long adminId = UserContext.requireAdmin();
        return Result.ok(Map.of("generated", adminService.regenThumbs(adminId)));
    }
}
