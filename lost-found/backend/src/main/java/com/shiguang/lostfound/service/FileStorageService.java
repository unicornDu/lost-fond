package com.shiguang.lostfound.service;

import cn.hutool.core.img.ImgUtil;
import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.config.FileProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 文件存储服务（风险预置#7 + 审查 2.16/2.17）。
 * - 图片存磁盘 uploads/yyyy/MM/uuid.ext，库中只存相对路径。
 * - 审查 2.16：除扩展名白名单外，校验文件头魔数，拒绝"改名为 jpg 的文本/脚本"。
 * - 审查 2.17：落盘时同步生成宽 480 缩略图 uuid_thumb.ext，列表用缩略图、详情用原图。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileProperties fileProperties;

    private static final java.util.Set<String> ALLOWED = java.util.Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final int THUMB_WIDTH = 480;

    /** 文件头魔数（审查 2.16 附录 C）。 */
    private static final Map<String, byte[]> MAGIC = Map.of(
            "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "gif", "GIF8".getBytes(StandardCharsets.US_ASCII),
            "bmp", new byte[]{0x42, 0x4D}
    );

    private Path root() {
        return Paths.get(fileProperties.getUploadDir()).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("上传文件为空");
        try {
            return store(file.getBytes(), file.getOriginalFilename());
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new BusinessException("图片读取失败");
        }
    }

    /** 保存字节：校验扩展名+魔数 → 写原图 → 生成缩略图。返回原图相对 URL。 */
    public String store(byte[] data, String originalFilename) {
        if (data == null || data.length == 0) throw new BusinessException("上传文件为空");
        String ext = extOf(originalFilename);
        if (!ALLOWED.contains(ext)) throw new BusinessException("仅支持 jpg/png/gif/bmp/webp 图片");
        checkMagic(data, ext);

        String sub = LocalDate.now().format(YM);
        String name = UUID.randomUUID().toString().replace("-", "");
        try {
            Path dir = root().resolve(sub);
            Files.createDirectories(dir);
            Files.write(dir.resolve(name + "." + ext), data);
            writeThumb(data, dir, name, ext);   // 审查 2.17 缩略图
            return "/uploads/" + sub + "/" + name + "." + ext;
        } catch (IOException e) {
            log.error("保存上传文件失败", e);
            throw new BusinessException("图片保存失败");
        }
    }

    /** 魔数校验：内容必须是真图片（审查 2.16）。webp 单独处理 RIFF....WEBP。 */
    private void checkMagic(byte[] data, String ext) {
        if ("webp".equals(ext)) {
            boolean ok = data.length > 12
                    && new String(data, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(data, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
            if (!ok) throw new BusinessException("文件内容不是图片");
            return;
        }
        byte[] magic = MAGIC.get(ext);
        if (magic == null) return; // 其余已在 ALLOWED 内但无魔数表的不强制
        if (data.length < magic.length) throw new BusinessException("文件内容不是图片");
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) throw new BusinessException("文件内容不是图片");
        }
    }

    /** 生成宽 480 缩略图 uuid_thumb.ext；失败仅记日志不阻断主流程。 */
    private void writeThumb(byte[] data, Path dir, String name, String ext) {
        try {
            BufferedImage src = ImgUtil.read(new ByteArrayInputStream(data));
            if (src == null) return;
            float scale = (float) THUMB_WIDTH / src.getWidth();
            if (scale >= 1) return; // 原图已够小，不另存缩略图（thumbUrl 回退原图）
            ImgUtil.write(ImgUtil.scale(src, scale), dir.resolve(name + "_thumb." + ext).toFile());
        } catch (Exception e) {
            log.warn("生成缩略图失败（忽略）: {}", name, e);
        }
    }

    /** 由原图 URL 推导缩略图 URL（不存在时前端回退原图）。 */
    public String thumbUrl(String url) {
        if (url == null) return null;
        int dot = url.lastIndexOf('.');
        if (dot < 0) return url;
        String candidate = url.substring(0, dot) + "_thumb" + url.substring(dot);
        File f = resolve(candidate);
        return (f != null && f.exists()) ? candidate : url;
    }

    /** 为历史数据补缩略图（审查 2.17 的 regen-thumbs 用）。返回补生成数量。 */
    public int ensureThumbs() {
        java.util.concurrent.atomic.AtomicInteger n = new java.util.concurrent.atomic.AtomicInteger(0);
        try {
            Files.walk(root()).filter(p -> {
                String fn = p.getFileName().toString();
                return !fn.contains("_thumb") && fn.matches("(?i).*\\.(jpg|jpeg|png|gif|bmp|webp)");
            }).forEach(p -> {
                try {
                    String fn = p.getFileName().toString();
                    int dot = fn.lastIndexOf('.');
                    Path thumb = p.resolveSibling(fn.substring(0, dot) + "_thumb" + fn.substring(dot));
                    if (Files.exists(thumb)) return;
                    BufferedImage src = ImgUtil.read(p.toFile());
                    if (src == null) return;
                    float scale = (float) THUMB_WIDTH / src.getWidth();
                    if (scale >= 1) return;
                    ImgUtil.write(ImgUtil.scale(src, scale), thumb.toFile());
                    n.incrementAndGet();
                } catch (Exception ignored) { }
            });
        } catch (IOException e) {
            log.warn("遍历上传目录失败", e);
        }
        return n.get();
    }

    public File resolve(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isBlank()) return null;
        String rel = relativeUrl.startsWith("/uploads/") ? relativeUrl.substring("/uploads/".length()) : relativeUrl;
        Path p = root().resolve(rel).normalize();
        if (!p.startsWith(root())) throw new BusinessException("非法文件路径");
        return p.toFile();
    }

    public byte[] readBytes(String relativeUrl) {
        File f = resolve(relativeUrl);
        if (f == null || !f.exists()) return null;
        try {
            return Files.readAllBytes(f.toPath());
        } catch (IOException e) {
            return null;
        }
    }

    /** 删除磁盘文件（审查 2.20 删除启事清理孤儿图片）；失败仅记日志。 */
    public void deleteQuietly(String relativeUrl) {
        try {
            File f = resolve(relativeUrl);
            if (f != null && f.exists()) Files.deleteIfExists(f.toPath());
            File t = resolve(thumbUrl(relativeUrl));
            if (t != null && t.exists() && !t.equals(f)) Files.deleteIfExists(t.toPath());
        } catch (Exception e) {
            log.warn("删除磁盘图片失败（忽略）: {}", relativeUrl, e);
        }
    }

    private String extOf(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i < 0 ? "" : filename.substring(i + 1).toLowerCase(Locale.ROOT);
    }
}
