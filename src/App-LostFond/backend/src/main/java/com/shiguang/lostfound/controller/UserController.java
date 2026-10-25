package com.shiguang.lostfound.controller;

import com.shiguang.lostfound.common.PageResult;
import com.shiguang.lostfound.common.Result;
import com.shiguang.lostfound.context.UserContext;
import com.shiguang.lostfound.dto.ChangePasswordReq;
import com.shiguang.lostfound.dto.DmReq;
import com.shiguang.lostfound.dto.UpdateProfileReq;
import com.shiguang.lostfound.entity.Message;
import com.shiguang.lostfound.service.FavoriteService;
import com.shiguang.lostfound.service.ItemService;
import com.shiguang.lostfound.service.MessageService;
import com.shiguang.lostfound.service.StatsService;
import com.shiguang.lostfound.service.UserService;
import com.shiguang.lostfound.vo.ItemVO;
import com.shiguang.lostfound.vo.StatsVO;
import com.shiguang.lostfound.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 个人中心：站内消息、首页统计、账号设置（E-01）、他人公开主页（E-04）。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final MessageService messageService;
    private final StatsService statsService;
    private final UserService userService;
    private final ItemService itemService;
    private final FavoriteService favoriteService;

    // ---------------- E-04：他人公开主页（登录可见；脱敏，无 account） ----------------

    @GetMapping("/users/{id}/profile")
    public Result<Map<String, Object>> userProfile(@PathVariable Long id) {
        UserContext.requireUserId();
        return Result.ok(userService.publicProfile(id));
    }

    @GetMapping("/users/{id}/items")
    public Result<PageResult<ItemVO>> userItems(@PathVariable Long id,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size) {
        UserContext.requireUserId();
        return Result.ok(itemService.listPublicByUser(id, page, size));
    }

    // ---------------- E-07：我的收藏 ----------------

    @GetMapping("/user/favorites")
    public Result<PageResult<ItemVO>> favorites(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "12") long size) {
        return Result.ok(favoriteService.list(UserContext.requireUserId(), page, size));
    }

    /** E-01：修改密码。成功后前端清 session 强制重新登录（无服务端吊销，既定取舍）。 */
    @PutMapping("/user/password")
    public Result<Void> changePassword(@RequestBody @Valid ChangePasswordReq req) {
        userService.changePassword(UserContext.requireUserId(), req);
        return Result.ok();
    }

    /** E-01：资料编辑（昵称/头像/手机号，部分更新）。 */
    @PutMapping("/user/profile")
    public Result<UserVO> updateProfile(@RequestBody @Valid UpdateProfileReq req) {
        return Result.ok(userService.updateProfile(UserContext.requireUserId(), req));
    }

    @GetMapping("/stats")
    public Result<StatsVO> stats() {
        return Result.ok(statsService.homeStats());
    }

    /** v2.41.1：首页信息总览（活动日历）。days 默认 182 天 ≈ 26 周。 */
    @GetMapping("/stats/daily")
    public Result<List<Map<String, Object>>> dailyStats(@RequestParam(defaultValue = "182") int days) {
        return Result.ok(statsService.dailyActivity(days));
    }

    @GetMapping("/user/messages")
    public Result<List<Message>> messages() {
        return Result.ok(messageService.list(UserContext.requireUserId()));
    }

    @GetMapping("/user/messages/unread")
    public Result<Map<String, Object>> unread() {
        return Result.ok(Map.of("count", messageService.unreadCount(UserContext.requireUserId())));
    }

    @PostMapping("/user/messages/read-all")
    public Result<Void> readAll() {
        messageService.markAllRead(UserContext.requireUserId());
        return Result.ok();
    }

    /** 审查 2.7 补：单条已读。 */
    @PostMapping("/user/messages/{id}/read")
    public Result<Void> readOne(@PathVariable Long id) {
        messageService.markRead(id, UserContext.requireUserId());
        return Result.ok();
    }

    /** E-03：回复一条私信（收件人本人，仅 dm，60 秒节流）。 */
    @PostMapping("/user/messages/{id}/reply")
    public Result<Void> replyMessage(@PathVariable Long id, @RequestBody @Valid DmReq req) {
        messageService.reply(id, UserContext.requireUserId(), req == null ? null : req.getContent());
        return Result.ok();
    }
}
