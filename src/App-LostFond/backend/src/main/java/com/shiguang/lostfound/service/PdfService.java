package com.shiguang.lostfound.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.entity.ClaimRecord;
import com.shiguang.lostfound.entity.Item;
import com.shiguang.lostfound.entity.User;
import com.shiguang.lostfound.vo.ItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 交付 PDF 生成器（桌面端 P1；纯 OpenPDF 模板填充，无 AI 依赖——
 * 对应《文件交付能力落地流程》§3.3 的"模板 + 数据填充"路线）。
 *
 * 中文字体：按 Windows/Linux 常见路径探测 CJK 字体（本课程部署环境为 Windows，
 * 优先微软雅黑；Linux 服务器部署见 docs/12 的字体说明），找不到即明确报错而非静默乱码。
 */
@Slf4j
@Service
public class PdfService {

    private static final Color BRAND = new Color(0xFF, 0x7D, 0x2E);
    private static final Color INK = new Color(0x33, 0x29, 0x1F);
    private static final Color LINE = new Color(0xE8, 0xDE, 0xD0);
    private static final DateTimeFormatter OUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static volatile BaseFont cachedFont;

    /** CJK 字体探测：Windows → Linux 常见路径依次尝试，全部失败明确报错。 */
    private BaseFont cjkFont() {
        BaseFont f = cachedFont;
        if (f != null) return f;
        String[] candidates = {
                "C:/Windows/Fonts/msyh.ttc,0",
                "C:/Windows/Fonts/simhei.ttf",
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc,0",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
        };
        for (String p : candidates) {
            try {
                f = BaseFont.createFont(p, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                cachedFont = f;
                return f;
            } catch (Exception ignored) { }
        }
        throw new BusinessException(500, "服务器缺少中文字体，无法生成 PDF（见 docs/12 字体说明）");
    }

    private Font font(float size, int style, Color color) {
        return new Font(cjkFont(), size, style, color);
    }

    /** 认领证明（CLAIM_CERT）：认领通过时由 ClaimService 生成，交付失主与认领人各一份。 */
    public byte[] claimCertificate(Item item, ClaimRecord claim, User owner, User claimant) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 56, 56, 64, 56);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph brand = new Paragraph("拾光 · 校园失物招领平台", font(11, Font.NORMAL, Color.GRAY));
            brand.setAlignment(Element.ALIGN_RIGHT);
            doc.add(brand);
            Paragraph title = new Paragraph("认 领 证 明", font(26, Font.BOLD, INK));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(18);
            title.setSpacingAfter(4);
            doc.add(title);
            Paragraph no = new Paragraph("编号：SG-" + String.format("%06d", claim.getId()),
                    font(10, Font.NORMAL, Color.GRAY));
            no.setAlignment(Element.ALIGN_CENTER);
            no.setSpacingAfter(18);
            doc.add(no);

           kvTable(doc,
                    "物品名称", item.getTitle(),
                    "物品分类", safe(item.getCategory()),
                    "物品地点", safe(item.getLocation()),
                    "发生时间", item.getHappenTime() != null ? item.getHappenTime().format(OUT) : "—",
                    "失主", nameOf(owner),
                    "认领人", nameOf(claimant),
                    "认领凭证摘要", abbreviate(claim.getProof(), 80),
                    "审核通过时间", claim.getUpdateTime() != null ? claim.getUpdateTime().format(OUT) : LocalDateTime.now().format(OUT));

            Paragraph note = new Paragraph(
                    "本证明由拾光平台依据认领审核记录自动生成，仅作为校园物品认领的交接凭证使用。",
                    font(9, Font.NORMAL, Color.GRAY));
            note.setSpacingBefore(24);
            doc.add(note);

            doc.close();
            return out.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("认领证明 PDF 生成失败 claimId={}", claim.getId(), e);
            throw new BusinessException(500, "认领证明生成失败");
        }
    }

    /** 匹配报告（MATCH_REPORT）：以图搜图完成后生成 Top-N 结果报告（绑定核心亮点 M3）。 */
    public byte[] matchReport(String queryFileName, int threshold, int topN,
                              long indexedCount, List<ItemVO> hits) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 56, 56, 64, 56);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph brand = new Paragraph("拾光 · 校园失物招领平台", font(11, Font.NORMAL, Color.GRAY));
            brand.setAlignment(Element.ALIGN_RIGHT);
            doc.add(brand);
            Paragraph title = new Paragraph("以图搜图匹配报告", font(22, Font.BOLD, INK));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(16);
            title.setSpacingAfter(14);
            doc.add(title);

            kvTable(doc,
                    "查询图片", safe(queryFileName),
                    "相似判定阈值", "汉明距离 ≤ " + threshold,
                    "返回上限 Top-N", String.valueOf(topN),
                    "库内比对图片数", String.valueOf(indexedCount),
                    "命中结果数", String.valueOf(hits.size()) + " 条",
                    "生成时间", LocalDateTime.now().format(OUT));

            PdfPTable table = new PdfPTable(new float[]{0.8f, 3.4f, 1.4f, 2.2f, 1.2f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(14);
            table.setHeaderRows(1);
            for (String h : new String[]{"#", "启事标题", "相似度", "地点", "状态"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, font(10, Font.BOLD, Color.WHITE)));
                c.setBackgroundColor(BRAND);
                c.setBorderColor(LINE);
                c.setPadding(6);
                table.addCell(c);
            }
            int i = 1;
            for (ItemVO v : hits) {
                Object[] row = {String.valueOf(i++), v.getTitle(),
                        String.format("%.1f%%", v.getSimilarity() == null ? 0 : v.getSimilarity()),
                        safe(v.getLocation()), safe(v.getStatus())};
                for (Object cell : row) {
                    PdfPCell c = new PdfPCell(new Phrase(String.valueOf(cell), font(9.5f, Font.NORMAL, INK)));
                    c.setBorderColor(LINE);
                    c.setPadding(6);
                    table.addCell(c);
                }
            }
            doc.add(table);

            Paragraph note = new Paragraph(
                    "相似度 = 1 − 汉明距离 / 64（64 位 pHash 感知哈希）；结果按相似度降序排列，仅供线下核对参考。",
                    font(9, Font.NORMAL, Color.GRAY));
            note.setSpacingBefore(14);
            doc.add(note);

            doc.close();
            return out.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("匹配报告 PDF 生成失败", e);
            throw new BusinessException(500, "匹配报告生成失败");
        }
    }

    // ---------------- 组装工具 ----------------

    private void kvTable(Document doc, String... kv) throws Exception {
        PdfPTable table = new PdfPTable(new float[]{1.1f, 2.9f});
        table.setWidthPercentage(100);
        for (int i = 0; i < kv.length; i += 2) {
            PdfPCell k = new PdfPCell(new Phrase(kv[i], font(10, Font.BOLD, INK)));
            k.setBackgroundColor(new Color(0xFF, 0xF3, 0xE6));
            k.setBorderColor(LINE);
            k.setPadding(7);
            table.addCell(k);
            PdfPCell v = new PdfPCell(new Phrase(kv[i + 1], font(10, Font.NORMAL, INK)));
            v.setBorderColor(LINE);
            v.setPadding(7);
            table.addCell(v);
        }
        doc.add(table);
    }

    private String nameOf(User u) {
        return u == null ? "同学" : safe(u.getNickname()) + "（" + safe(u.getAccount()) + "）";
    }

    private String safe(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private String abbreviate(String s, int max) {
        if (s == null || s.isBlank()) return "—";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
