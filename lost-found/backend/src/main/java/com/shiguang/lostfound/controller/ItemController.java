package com.shiguang.lostfound.controller;

import com.shiguang.lostfound.common.PageResult;
import com.shiguang.lostfound.common.Result;
import com.shiguang.lostfound.context.UserContext;
import com.shiguang.lostfound.dto.ItemForm;
import com.shiguang.lostfound.service.ItemService;
import com.shiguang.lostfound.service.SearchService;
import com.shiguang.lostfound.vo.ItemVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * M2 发布 + M4 检索 + 2.7 私信 + 2.26 相似推荐。
 */
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final SearchService searchService;

    @GetMapping
    public Result<PageResult<ItemVO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "new") String sort,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "12") long size) {
        return Result.ok(itemService.list(type, category, location, keyword, sort, page, size));
    }

    @GetMapping("/{id}")
    public Result<ItemVO> detail(@PathVariable Long id, HttpServletRequest request) {
        Long uid = UserContext.getUserId(); // 可匿名浏览
        String clientKey = uid != null ? ("u" + uid) : request.getRemoteAddr();
        return Result.ok(itemService.detail(id, uid, clientKey));
    }

    /** 审查 2.26：真·视觉相似推荐（复用同一条 pHash 汉明距离 SQL 查最近邻）。 */
    @GetMapping("/{id}/similar")
    public Result<List<ItemVO>> similar(@PathVariable Long id,
                                        @RequestParam(defaultValue = "3") int n) {
        return Result.ok(searchService.similarTo(id, n));
    }

    @GetMapping("/my")
    public Result<List<ItemVO>> my() {
        return Result.ok(itemService.myPublishes(UserContext.requireUserId()));
    }

    /** 发布启事：multipart 表单字段 + files 图片（后端逐张校验魔数、算 pHash、生成缩略图）。 */
    @PostMapping(consumes = {"multipart/form-data"})
    public Result<ItemVO> publish(@ModelAttribute @Valid ItemForm form,
                                  @RequestPart(value = "files", required = false) MultipartFile[] files) {
        Long uid = UserContext.requireUserId();
        return Result.ok(itemService.publish(uid, form, files));
    }

    @PostMapping("/{id}/mark-found")
    public Result<Void> markFound(@PathVariable Long id) {
        itemService.markFound(id, UserContext.requireUserId());
        return Result.ok();
    }

    /** 审查 2.7：私信接真——给启事发布者发站内消息。 */
    @PostMapping("/{id}/dm")
    public Result<Void> dm(@PathVariable Long id, @RequestBody Map<String, String> body) {
        itemService.sendDm(id, UserContext.requireUserId(), body == null ? null : body.get("content"));
        return Result.ok();
    }
}
