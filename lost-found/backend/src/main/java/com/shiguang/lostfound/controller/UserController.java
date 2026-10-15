package com.shiguang.lostfound.controller;

import com.shiguang.lostfound.common.Result;
import com.shiguang.lostfound.context.UserContext;
import com.shiguang.lostfound.entity.Message;
import com.shiguang.lostfound.service.MessageService;
import com.shiguang.lostfound.service.StatsService;
import com.shiguang.lostfound.vo.StatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 个人中心：站内消息、首页统计。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final MessageService messageService;
    private final StatsService statsService;

    @GetMapping("/stats")
    public Result<StatsVO> stats() {
        return Result.ok(statsService.homeStats());
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
}
