package com.shiguang.lostfound.controller;

import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.common.Result;
import com.shiguang.lostfound.context.UserContext;
import com.shiguang.lostfound.entity.Delivery;
import com.shiguang.lostfound.service.FileDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 受控文件路由（P1 文件交付）。
 * 全部在 JwtInterceptor 围栏之后；浏览器只传 deliveryId，路径由服务端解析，
 * 永不下发绝对路径（原桌面壳用的 /open 已随 Electron 端下线一并移除）。
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileDeliveryService deliveryService;

    /** 我的交付列表（消息中心卡片渲染，读表而非问业务方——dsh 解耦原则）。 */
    @GetMapping("/list")
    public Result<Object> list() {
        return Result.ok(deliveryService.list(UserContext.requireUserId()));
    }

    /** 受控下载：三件套响应头（no-store / nosniff / CSP sandbox）+ UTF-8 文件名。 */
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam Long id) {
        Long uid = UserContext.requireUserId();
        Delivery d = deliveryService.getReadable(id, uid);
        Path p = deliveryService.resolveReal(d);
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(p);
        } catch (Exception e) {
            throw new BusinessException(404, "交付文件不存在或已失效");
        }
        String fn = URLEncoder.encode(d.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename*=UTF-8''" + fn)
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "private, no-store")
                .header("Content-Security-Policy", "sandbox; default-src 'none'")
                .contentType(MediaType.parseMediaType(d.getMimeType()))
                .contentLength(bytes.length)
                .body(bytes);
    }

    /** 本人删除自己的交付（行 + 盘；供用户清理与测试幂等）。 */
    @DeleteMapping
    public Result<Void> delete(@RequestParam Long id) {
        deliveryService.deleteOwn(id, UserContext.requireUserId());
        return Result.ok();
    }
}
