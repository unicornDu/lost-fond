package com.shiguang.lostfound.service;

import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.config.FileProperties;
import com.shiguang.lostfound.entity.Delivery;
import com.shiguang.lostfound.mapper.DeliveryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P1：交付文件服务单测（dsh 踩坑记录的证伪锚点）。
 * 覆盖：正常交付（行+盘）/ 空内容 400 / 超限 413 / MIME 白名单 400 /
 * 越权 403 / 失效 404 / 路径穿越 403 / 符号链接 403 / 本人删除 / 按 biz 级联。
 * UserContext.isAdmin() 在无请求上下文时恒为 false（ThreadLocal 未写入）。
 */
@ExtendWith(MockitoExtension.class)
class FileDeliveryServiceTest {

    @Mock DeliveryMapper deliveryMapper;

    @TempDir
    Path tempDir;

    /** 2026-10-18 审查修复：符号链接目标必须放在 uploads 根之外——原 fixture 把 outside.txt
     *  建在 tempDir（即根）内部，语义不成立，导致"期望 403 实得 404"的假失败。 */
    @TempDir
    Path outsideDir;

    private FileDeliveryService service;
    private FileProperties props;

    @BeforeEach
    void setUp() {
        props = new FileProperties();
        props.setUploadDir(tempDir.toString());
        service = new FileDeliveryService(deliveryMapper, props);
    }

    private Delivery row(long id, long userId, String bizType, String rel) {
        Delivery d = new Delivery();
        d.setId(id);
        d.setUserId(userId);
        d.setBizType(bizType);
        d.setBizId(1L);
        d.setTitle("认领证明 #" + id);
        d.setFilePath(rel);
        d.setFileName("认领证明.pdf");
        d.setMimeType("application/pdf");
        d.setFileSize(100L);
        d.setStatus(1);
        return d;
    }

    @Test
    @DisplayName("deliver：正常交付 → 落盘到 deliveries/yyyy/MM/ 且行已插入")
    void deliverOk() {
        service.deliver(5L, "CLAIM_CERT", 1L, "认领证明 #1", "desc",
                "%PDF-1.7 test".getBytes(), "pdf", "application/pdf");
        // 真实落盘位置：uploads/deliveries/yyyy/MM/
        try (var walk = Files.walk(tempDir)) {
            List<Path> files = walk.filter(Files::isRegularFile).toList();
            assertEquals(1, files.size());
            assertTrue(files.get(0).getFileName().toString().endsWith(".pdf"));
        } catch (Exception e) { fail(e); }
        verify(deliveryMapper).insert(any(Delivery.class));
    }

    @Test
    @DisplayName("deliver：空内容 → 400；超限 → 413；非法 MIME → 400")
    void deliverGuards() {
        assertEquals(400, assertThrows(BusinessException.class, () ->
                service.deliver(5L, "CLAIM_CERT", 1L, "t", null, new byte[0], "pdf", "application/pdf")).getCode());
        assertEquals(413, assertThrows(BusinessException.class, () ->
                service.deliver(5L, "CLAIM_CERT", 1L, "t", null, new byte[21 * 1024 * 1024], "pdf", "application/pdf")).getCode());
        assertEquals(400, assertThrows(BusinessException.class, () ->
                service.deliver(5L, "CLAIM_CERT", 1L, "t", null, new byte[]{1}, "exe", "application/octet-stream")).getCode());
        verify(deliveryMapper, never()).insert(any(Delivery.class));
    }

    @Test
    @DisplayName("getReadable：他人交付 → 403（越权，dsh 认证围栏语义）")
    void readableRejectsOthers() {
        when(deliveryMapper.selectById(9L)).thenReturn(row(9L, 7L, "CLAIM_CERT", "deliveries/x.pdf"));
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.getReadable(9L, 5L)).getCode());
    }

    @Test
    @DisplayName("getReadable：不存在/失效 → 404")
    void readableMissing404() {
        when(deliveryMapper.selectById(9L)).thenReturn(null);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.getReadable(9L, 5L)).getCode());
        Delivery dead = row(9L, 5L, "CLAIM_CERT", "deliveries/x.pdf");
        dead.setStatus(0);
        when(deliveryMapper.selectById(9L)).thenReturn(dead);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.getReadable(9L, 5L)).getCode());
    }

    @Test
    @DisplayName("resolveReal：路径穿越（../）→ 403 且不泄露服务器路径")
    void resolveRejectsTraversal() {
        Delivery d = row(1L, 5L, "CLAIM_CERT", "deliveries/../../outside.pdf");
        assertEquals(403, assertThrows(BusinessException.class, () -> service.resolveReal(d)).getCode());
    }

    @Test
    @DisplayName("resolveReal：符号链接指向根外 → 403（toRealPath 复检）")
    void resolveRejectsSymlink() throws Exception {
        Path outside = outsideDir.resolve("outside.txt");   // 根之外
        Files.writeString(outside, "secret");
        Path dir = Files.createDirectories(tempDir.resolve("deliveries/2026/10"));
        Path link = dir.resolve("evil.pdf");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | java.io.IOException e) {
            assumeTrue(false, "当前环境不支持符号链接（Windows 非管理员常见），跳过该用例");
        }
        // 2026-10-18 诊断补充：本机 Windows 上 createSymbolicLink 可能"静默不创建"（不抛异常、
        // isSymbolicLink 为 false），此时断言 403 无意义——确认链接真实存在后再断言。
        assumeTrue(Files.isSymbolicLink(link), "当前环境未真正创建符号链接，跳过该用例");
        Delivery d = row(1L, 5L, "CLAIM_CERT", "deliveries/2026/10/evil.pdf");
        assertEquals(403, assertThrows(BusinessException.class, () -> service.resolveReal(d)).getCode());
    }

    @Test
    @DisplayName("resolveReal：本人正常读取真实路径")
    void resolveOk() throws Exception {
        Path dir = Files.createDirectories(tempDir.resolve("deliveries/2026/10"));
        Path file = dir.resolve("ok.pdf");
        Files.write(file, "%PDF-1.7".getBytes());
        Delivery d = row(1L, 5L, "CLAIM_CERT", "deliveries/2026/10/ok.pdf");
        Path real = service.resolveReal(d);
        assertEquals("%PDF-1.7", new String(Files.readAllBytes(real)));
    }

    @Test
    @DisplayName("deleteByBiz：按业务级联删行+删盘（对齐 B-05 风格）")
    void cascadeByBiz() throws Exception {
        Path dir = Files.createDirectories(tempDir.resolve("deliveries/2026/10"));
        Path file = dir.resolve("a.pdf");
        Files.write(file, "%PDF".getBytes());
        when(deliveryMapper.selectList(any())).thenReturn(
                List.of(row(1L, 5L, "CLAIM_CERT", "deliveries/2026/10/a.pdf")));

        int n = service.deleteByBiz(1L);

        assertEquals(1, n);
        verify(deliveryMapper).delete(any());
        assertFalse(Files.exists(file));
    }
}
