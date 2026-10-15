package com.shiguang.lostfound.controller;

import com.shiguang.lostfound.common.Result;
import com.shiguang.lostfound.context.UserContext;
import com.shiguang.lostfound.service.SearchService;
import com.shiguang.lostfound.vo.ItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * M3 以图搜图（核心亮点）：上传图片 → pHash → 汉明距离 Top-N。
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping(value = "/image", consumes = {"multipart/form-data"})
    public Result<List<ItemVO>> searchByImage(@RequestPart("file") MultipartFile file) {
        return Result.ok(searchService.searchByImage(file));
    }

    /** 已建立 pHash 索引的图片数量（前端展示「已在 N 张图片中比对」）。 */
    @GetMapping("/indexed-count")
    public Result<Map<String, Object>> indexedCount() {
        return Result.ok(Map.of("count", searchService.indexedCount()));
    }

    /** 管理/调试：为漏算 pHash 的图片补算指纹。审查 2.19：必须管理员（匿名可触发全表重算）。 */
    @PostMapping("/reindex")
    public Result<Map<String, Object>> reindex() {
        UserContext.requireAdmin();
        int n = searchService.reindex();
        return Result.ok(Map.of("reindexed", n, "total", searchService.indexedCount()));
    }
}
