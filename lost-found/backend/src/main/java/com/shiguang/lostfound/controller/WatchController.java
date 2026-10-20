package com.shiguang.lostfound.controller;

import com.shiguang.lostfound.common.Result;
import com.shiguang.lostfound.context.UserContext;
import com.shiguang.lostfound.dto.WatchSubscribeReq;
import com.shiguang.lostfound.service.WatchService;
import com.shiguang.lostfound.vo.WatchOverviewVO;
import com.shiguang.lostfound.vo.WatchPlanVO;
import com.shiguang.lostfound.vo.WatchSubscriptionVO;
import com.shiguang.lostfound.vo.WatchTargetVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * v2.20 守望订阅：方案目录、当前订阅与守望位、以图搜图页开启守望、结束守望。
 * 鉴权由 WebMvcConfig 的 /api/** 拦截器统一负责（除登录注册外都要求登录）。
 */
@RestController
@RequestMapping("/api/watch")
@RequiredArgsConstructor
public class WatchController {

    private final WatchService watchService;

    /** 方案目录（前端按 code 走 i18n，只取数值权益）。 */
    @GetMapping("/plans")
    public Result<List<WatchPlanVO>> plans() {
        return Result.ok(watchService.plans());
    }

    /** 当前订阅 + 已用守望位 + 守望位列表。 */
    @GetMapping("/me")
    public Result<WatchOverviewVO> me() {
        return Result.ok(watchService.overview(UserContext.requireUserId()));
    }

    /** 开通 / 切换方案（课程演示：只落库权益，不扣费）。 */
    @PostMapping("/subscribe")
    public Result<WatchSubscriptionVO> subscribe(@RequestBody @Valid WatchSubscribeReq req) {
        return Result.ok(watchService.subscribe(
                UserContext.requireUserId(), req.getPlanCode(), req.getCycle()));
    }

    /** 以图搜图页「守望订阅」开关：上传照片 → 服务端算 pHash 并占用一个守望位。 */
    @PostMapping(value = "/targets", consumes = {"multipart/form-data"})
    public Result<WatchTargetVO> addTarget(@RequestPart("file") MultipartFile file,
                                           @RequestParam(required = false) String label,
                                           @RequestParam(required = false) Long sourceItemId) {
        return Result.ok(watchService.addTarget(
                UserContext.requireUserId(), file, label, sourceItemId));
    }

    /** 结束守望（不删历史行，只置为已结束）。 */
    @DeleteMapping("/targets/{id}")
    public Result<Void> removeTarget(@PathVariable Long id) {
        watchService.removeTarget(UserContext.requireUserId(), id);
        return Result.ok();
    }
}
