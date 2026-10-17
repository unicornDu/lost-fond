package com.shiguang.lostfound.service;

import com.shiguang.lostfound.entity.ClaimRecord;
import com.shiguang.lostfound.entity.Item;
import com.shiguang.lostfound.entity.User;
import com.shiguang.lostfound.vo.ItemVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P1：PDF 生成器单测（不校验版式，锚定"产物是合法 PDF + 中文字体可用 + 关键内容进文"）。
 * 运行环境需有 CJK 字体（Windows 微软雅黑/黑体必有；Linux 见 docs/12 字体说明）。
 */
class PdfServiceTest {

    private final PdfService service = new PdfService();

    private Item item() {
        Item it = new Item();
        it.setId(1L);
        it.setTitle("蓝色保温杯");
        it.setCategory("电子设备");
        it.setLocation("图书馆三楼");
        it.setHappenTime(LocalDateTime.of(2026, 10, 17, 9, 30));
        it.setType("found");
        return it;
    }

    private ClaimRecord claim() {
        ClaimRecord c = new ClaimRecord();
        c.setId(12L);
        c.setProof("杯盖内侧有裂纹，杯底贴姓名贴");
        c.setUpdateTime(LocalDateTime.of(2026, 10, 17, 10, 0));
        return c;
    }

    private User user(long id, String account, String nick) {
        User u = new User();
        u.setId(id);
        u.setAccount(account);
        u.setNickname(nick);
        return u;
    }

    @Test
    @DisplayName("claimCertificate：产物为合法 PDF（%PDF 头）且字节数合理")
    void claimCertIsPdf() {
        byte[] pdf = service.claimCertificate(item(), claim(),
                user(9L, "20240009", "失主同学"), user(7L, "20240007", "认领同学"));
        String head = new String(pdf, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", head);
        assertTrue(pdf.length > 2000, "PDF 应含嵌入字体，字节数应明显大于空壳: " + pdf.length);
    }

    @Test
    @DisplayName("matchReport：产物为合法 PDF，含命中行数")
    void matchReportIsPdf() {
        ItemVO vo = new ItemVO();
        vo.setId(1L);
        vo.setTitle("蓝色保温杯");
        vo.setSimilarity(96.5);
        vo.setLocation("图书馆三楼");
        vo.setStatus("待认领");
        byte[] pdf = service.matchReport("q-cup.jpg", 12, 10, 120L, List.of(vo));
        String head = new String(pdf, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", head);
        assertTrue(pdf.length > 2000);
    }
}
