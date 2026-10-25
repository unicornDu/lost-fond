package com.shiguang.lostfound.controller;

import com.shiguang.lostfound.common.Result;
import com.shiguang.lostfound.context.UserContext;
import com.shiguang.lostfound.service.FileDeliveryService;
import com.shiguang.lostfound.service.PdfService;
import com.shiguang.lostfound.service.SearchService;
import com.shiguang.lostfound.vo.ItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * M3 以图搜图（核心亮点）：上传图片 → pHash → 汉明距离 Top-N。
 * 桌面端 P2：匹配报告 PDF 生成（入 delivery 体系）。
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final com.shiguang.lostfound.config.ImageSearchProperties imageSearchProperties;
    private final PdfService pdfService;
    private final FileDeliveryService fileDeliveryService;

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

    /** 桌面端 P2：以图搜图匹配报告 PDF（入 delivery 体系，交付给当前用户）。
     *  与 /image 同 clamp 口径（Z-08）；需登录（交付要落到具体用户）。 */
    @PostMapping(value = "/match-report", consumes = {"multipart/form-data"})
    public Result<Map<String, Object>> matchReport(@RequestPart("file") MultipartFile file,
                                                   @RequestParam(required = false) Integer threshold,
                                                   @RequestParam(required = false) Integer topN) {
        Long uid = UserContext.requireUserId();
        List<ItemVO> hits = searchService.searchByImage(file, threshold, topN);
        // 与 searchByImage 相同的 clamp 口径（P1 复用注释：allowOverride=false 时入参无效）
        boolean overridable = imageSearchProperties.isAllowOverride();
        int th = overridable && threshold != null
                ? Math.min(64, Math.max(0, threshold)) : imageSearchProperties.getHammingThreshold();
        int tn = overridable && topN != null
                ? Math.min(50, Math.max(1, topN)) : imageSearchProperties.getTopN();

        byte[] pdf = pdfService.matchReport(file.getOriginalFilename(), th, tn,
                searchService.indexedCount(), hits);
        var d = fileDeliveryService.deliver(uid, "MATCH_REPORT", null,
                "匹配报告 " + java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")),
                "以图搜图命中 " + hits.size() + " 条（阈值 " + th + " / Top-" + tn + "）",
                pdf, "pdf", "application/pdf");
        return Result.ok(Map.of("deliveryId", d.getId(), "title", d.getTitle(), "hits", hits.size()));
    }
}
