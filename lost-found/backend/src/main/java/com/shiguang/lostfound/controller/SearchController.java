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
    private final com.shiguang.lostfound.config.ImageSearchProperties imageSearchProperties;

    /** 03 T-02：threshold/topN 可选覆盖（eval_recall.py 网格扫描用）；不传用 application.yml 配置值。 */
    @PostMapping(value = "/image", consumes = {"multipart/form-data"})
    public Result<List<ItemVO>> searchByImage(@RequestPart("file") MultipartFile file,
                                              @RequestParam(required = false) Integer threshold,
                                              @RequestParam(required = false) Integer topN) {
        return Result.ok(searchService.searchByImage(file, threshold, topN));
    }

    /** 已建立 pHash 索引的图片数量 + 当前生效阈值/Top-N（前端文案同步用，03 T-03）。 */
    @GetMapping("/indexed-count")
    public Result<Map<String, Object>> indexedCount() {
        return Result.ok(Map.of(
                "count", searchService.indexedCount(),
                "threshold", imageSearchProperties.getHammingThreshold(),
                "topN", imageSearchProperties.getTopN()));
    }

    /** 管理/调试：为漏算 pHash 的图片补算指纹。审查 2.19：必须管理员（匿名可触发全表重算）。 */
    @PostMapping("/reindex")
    public Result<Map<String, Object>> reindex() {
        UserContext.requireAdmin();
        int n = searchService.reindex();
        return Result.ok(Map.of("reindexed", n, "total", searchService.indexedCount()));
    }
}
