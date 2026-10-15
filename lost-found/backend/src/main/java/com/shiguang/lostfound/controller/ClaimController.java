package com.shiguang.lostfound.controller;

import com.shiguang.lostfound.common.Result;
import com.shiguang.lostfound.context.UserContext;
import com.shiguang.lostfound.dto.ClaimAuditReq;
import com.shiguang.lostfound.dto.ClaimReq;
import com.shiguang.lostfound.service.ClaimService;
import com.shiguang.lostfound.vo.ClaimVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * M5 认领与审核。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    /** 对某启事提交认领申请。 */
    @PostMapping("/items/{id}/claim")
    public Result<Void> apply(@PathVariable Long id, @RequestBody @Valid ClaimReq req) {
        claimService.apply(id, UserContext.requireUserId(), req);
        return Result.ok();
    }

    /** 我发起的认领（个人中心「我的认领」）。 */
    @GetMapping("/claims/my")
    public Result<List<ClaimVO>> my() {
        return Result.ok(claimService.myClaims(UserContext.requireUserId()));
    }

    /** 我发布的启事收到的认领申请（待我审核）。 */
    @GetMapping("/claims/received")
    public Result<List<ClaimVO>> received() {
        return Result.ok(claimService.receivedClaims(UserContext.requireUserId()));
    }

    /** 发布者审核认领申请。 */
    @PostMapping("/claims/{claimId}/audit")
    public Result<Void> audit(@PathVariable Long claimId, @RequestBody ClaimAuditReq req) {
        claimService.audit(claimId, UserContext.requireUserId(), req);
        return Result.ok();
    }
}
