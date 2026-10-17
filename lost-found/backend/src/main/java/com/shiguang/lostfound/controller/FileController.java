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
import java.util.Map;

/**
 * 受控文件路由（桌面端 P1，dsh /api/file + present.open 转译）。
 * 全部在 JwtInterceptor 围栏之后；浏览器只传 deliveryId，路径由服务端读回；
 * /open 仅供桌面壳经 IPC 原生打开使用（壳侧再做 realpath 双保险校验）。
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

    /** 桌面联动：服务端按 id 读回绝对路径，壳经 IPC shell.openPath 原生打开（Web 端不调本端点）。 */
    @PostMapping("/open")
    public Result<Map<String, Object>> open(@RequestParam Long id) {
        Long uid = UserContext.requireUserId();
        Delivery d = deliveryService.getReadable(id, uid);
        Path p = deliveryService.resolveReal(d);
        return Result.ok(Map.of("path", p.toString(), "fileName", d.getFileName(), "mimeType", d.getMimeType()));
    }

    /** 本人删除自己的交付（行 + 盘；供用户清理与测试幂等）。 */
    @DeleteMapping
    public Result<Void> delete(@RequestParam Long id) {
        deliveryService.deleteOwn(id, UserContext.requireUserId());
        return Result.ok();
    }

    // ---------------- M-P1：Android APK 分发 ----------------

    /**
     * APK 下载（公开端点，无需登录——官网/答辩现场扫码安装入口）。
     * APK 由组员放置在 uploads/downloads/app-release.apk；按 M-P1 要求补安全响应头
     * （vnd.android.package-archive + attachment + nosniff，防 APK 被当网页执行）。
     */
    @GetMapping("/apk")
    public ResponseEntity<byte[]> apk() {
        Path p = deliveryService.resolveUploadRelative("downloads/app-release.apk");
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(p);
        } catch (Exception e) {
            throw new BusinessException(404, "APK 尚未发布，请联系管理员");
        }
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"shiguang-lostfound.apk\"")
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "private, no-store")
                .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
                .contentLength(bytes.length)
                .body(bytes);
    }
}
