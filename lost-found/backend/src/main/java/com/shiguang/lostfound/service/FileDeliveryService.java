package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.config.FileProperties;
import com.shiguang.lostfound.context.UserContext;
import com.shiguang.lostfound.entity.Delivery;
import com.shiguang.lostfound.mapper.DeliveryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 交付文件服务（桌面端 P1，dsh serveFile/present 转译）。
 *
 * 三条铁律（源自 dsh 踩坑记录）：
 * ① 浏览器/渲染层永远只传 deliveryId，路径由服务端按记录读回（防任意文件读取）；
 * ② 路径 resolve 后必须在 uploads 根内，且 toRealPath 后复检（防符号链接绕过白名单）；
 * ③ MIME 白名单窄化 + 字节上限（超限 413），错误码与 HTTP 状态一致（B-06 口径）。
 *
 * 交付文件落盘 uploads/deliveries/yyyy/MM/uuid.ext——桌面端与后端同机，
 * 文件生成即在用户磁盘上，"打开"走壳的原生 openPath（P2），Web 端走受控下载。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileDeliveryService {

    private final DeliveryMapper deliveryMapper;
    private final FileProperties fileProperties;

    /** MIME 白名单窄化（比 dsh 更严，本场景固定三类） */
    public static final List<String> ALLOWED_MIME =
            List.of("application/pdf", "text/csv", "application/zip");
    /** 生成上限 20MB（生成器产出的 PDF/CSV 远小于此，防御性兜底） */
    public static final long MAX_FILE_BYTES = 20L * 1024 * 1024;

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy/MM");

    private Path uploadsRoot() {
        return Paths.get(fileProperties.getUploadDir()).toAbsolutePath().normalize();
    }

    /**
     * 业务方唯一入口：生成文件后调用（只声明，不掺和下载/打开逻辑）。
     * 失败抛 BusinessException；调用方（如认领审核）自行决定是否阻断主流程。
     */
    @Transactional
    public Delivery deliver(Long userId, String bizType, Long bizId, String title,
                            String description, byte[] bytes, String ext, String mime) {
        if (userId == null) throw new BusinessException(400, "交付缺少接收人");
        if (bytes == null || bytes.length == 0) throw new BusinessException(400, "交付文件内容为空");
        if (bytes.length > MAX_FILE_BYTES) throw new BusinessException(413, "交付文件超过大小上限");
        if (!ALLOWED_MIME.contains(mime)) throw new BusinessException(400, "不支持的交付文件类型：" + mime);

        String sub = LocalDate.now().format(YM);
        String name = UUID.randomUUID().toString().replace("-", "");
        String rel = "deliveries/" + sub + "/" + name + "." + ext;
        try {
            Path dir = uploadsRoot().resolve(rel).getParent();
            Files.createDirectories(dir);
            Files.write(dir.resolve(name + "." + ext), bytes);
        } catch (IOException e) {
            log.error("交付文件落盘失败: {}", rel, e);
            throw new BusinessException(500, "交付文件写入失败");
        }

        Delivery d = new Delivery();
        d.setUserId(userId);
        d.setBizType(bizType);
        d.setBizId(bizId);
        d.setTitle(title);
        d.setDescription(description);
        d.setFilePath(rel);
        d.setFileName(title.replaceAll("[\\\\/:*?\"<>|]", "_") + "." + ext);
        d.setMimeType(mime);
        d.setFileSize((long) bytes.length);
        d.setStatus(1);
        deliveryMapper.insert(d);
        log.info("交付文件已生成: id={} user={} type={} size={}", d.getId(), userId, bizType, bytes.length);
        return d;
    }

    /** 受控读取守卫：存在且有效（404）、本人或管理员（403）。下载与原生打开共用。 */
    public Delivery getReadable(Long id, Long currentUserId) {
        Delivery d = deliveryMapper.selectById(id);
        if (d == null || d.getStatus() == null || d.getStatus() != 1)
            throw new BusinessException(404, "交付文件不存在或已失效");
        boolean admin = UserContext.isAdmin();
        if (!d.getUserId().equals(currentUserId) && !admin)
            throw new BusinessException(403, "只能访问自己的交付文件");
        return d;
    }

    /**
     * 由记录读回真实路径（防穿越/防符号链接）：
     * normalize 后必须在 uploads 根内，toRealPath 后复检一次。
     */
    public Path resolveReal(Delivery d) {
        Path root = uploadsRoot();
        Path p = root.resolve(d.getFilePath()).normalize();
        if (!p.startsWith(root)) throw new BusinessException(403, "非法文件路径");
        if (!Files.isRegularFile(p)) throw new BusinessException(404, "交付文件不存在或已失效");
        try {
            Path real = p.toRealPath();
            if (!real.startsWith(root.toRealPath())) throw new BusinessException(403, "非法文件路径");
            return real;
        } catch (IOException e) {
            throw new BusinessException(404, "交付文件不存在或已失效");
        }
    }

    /** 由 uploads 相对路径解析真实文件（供 APK 分发等静态资产用；同样做根前缀防穿越）。 */
    public Path resolveUploadRelative(String rel) {
        Path root = uploadsRoot();
        Path p = root.resolve(rel).normalize();
        if (!p.startsWith(root)) throw new BusinessException(403, "非法文件路径");
        if (!Files.isRegularFile(p)) throw new BusinessException(404, "文件不存在");
        return p;
    }

    /** 我的交付（卡片列表，上限 50，新→旧）。 */
    public List<Delivery> list(Long userId) {
        return deliveryMapper.selectList(new QueryWrapper<Delivery>()
                .eq("user_id", userId).eq("status", 1)
                .orderByDesc("create_time").last("LIMIT 50"));
    }

    /** 本人删除自己的交付（行 + 盘）。 */
    @Transactional
    public void deleteOwn(Long id, Long userId) {
        Delivery d = deliveryMapper.selectById(id);
        if (d == null || !d.getUserId().equals(userId)) throw new BusinessException(404, "交付文件不存在");
        deliveryMapper.deleteById(id);
        deleteQuietlyOnDisk(d);
    }

    /** 启事删除级联（对齐 B-05 风格）：按 biz_id 清 delivery 行并删盘。 */
    @Transactional
    public int deleteByBiz(Long bizId) {
        List<Delivery> rows = deliveryMapper.selectList(
                new QueryWrapper<Delivery>().eq("biz_id", bizId));
        if (rows.isEmpty()) return 0;
        deliveryMapper.delete(new QueryWrapper<Delivery>().eq("biz_id", bizId));
        rows.forEach(this::deleteQuietlyOnDisk);
        return rows.size();
    }

    private void deleteQuietlyOnDisk(Delivery d) {
        try {
            Files.deleteIfExists(resolveReal(d));
        } catch (Exception e) {
            log.warn("删除交付文件失败（忽略）: id={} path={}", d.getId(), d.getFilePath(), e);
        }
    }
}
