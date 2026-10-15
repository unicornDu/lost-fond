package com.shiguang.lostfound.util;

import com.shiguang.lostfound.config.ImageSearchProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审查 2.15：PHashUtil 单元测试（纯内存造图，不依赖磁盘/网络/数据库）。
 */
class PHashUtilTest {

    private PHashUtil pHashUtil;

    @BeforeEach
    void setUp() {
        ImageSearchProperties props = new ImageSearchProperties(); // 默认 64/10/6
        pHashUtil = new PHashUtil(props);
    }

    private byte[] toBytes(BufferedImage img) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }

    /** 造一张"渐变+色块"的测试图，seed 控制内容差异。 */
    private BufferedImage makeImage(int seed, float brightness) {
        int w = 128, h = 96;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color((seed * 37) % 200 + 30, (seed * 53) % 200 + 30, (seed * 71) % 200 + 30));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(255, 255, 255));
        g.fillOval(w / 4 + seed, h / 4, w / 2, h / 2);
        g.setColor(new Color(20, 20, 20));
        g.fillRect(w / 2 - 10 + seed, h / 2 - 10, 20, 20);
        g.dispose();
        if (brightness != 1.0f) {
            for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) {
                int rgb = img.getRGB(x, y);
                int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * brightness));
                int gg = Math.min(255, (int) (((rgb >> 8) & 0xFF) * brightness));
                int b = Math.min(255, (int) ((rgb & 0xFF) * brightness));
                img.setRGB(x, y, (r << 16) | (gg << 8) | b);
            }
        }
        return img;
    }

    @Test
    @DisplayName("同图两次哈希一致（确定性）")
    void sameImageConsistent() throws Exception {
        byte[] b = toBytes(makeImage(1, 1.0f));
        long h1 = pHashUtil.hash(b);
        long h2 = pHashUtil.hash(b);
        assertEquals(h1, h2);
        assertEquals(0, pHashUtil.hammingDistance(h1, h2));
    }

    @Test
    @DisplayName("亮度变体距离小于阈值（pHash 抗光照）")
    void brightnessVariantClose() throws Exception {
        long a = pHashUtil.hash(toBytes(makeImage(2, 1.0f)));
        long b = pHashUtil.hash(toBytes(makeImage(2, 1.15f)));
        int dist = pHashUtil.hammingDistance(a, b);
        assertTrue(dist <= 10, "亮度变体汉明距离应≤阈值10，实际=" + dist);
    }

    @Test
    @DisplayName("不同图像距离明显大于 0")
    void differentImagesFar() throws Exception {
        long a = pHashUtil.hash(toBytes(makeImage(3, 1.0f)));
        long b = pHashUtil.hash(toBytes(makeImage(9, 1.0f)));
        assertTrue(pHashUtil.hammingDistance(a, b) > 0);
    }

    @Test
    @DisplayName("汉明距离与相似度边界 0/64")
    void similarityBoundaries() {
        assertEquals(100.0, pHashUtil.similarityFromDist(0));
        assertEquals(0.0, pHashUtil.similarityFromDist(64));
        assertEquals(0, pHashUtil.hammingDistance(0L, 0L));
        assertEquals(64, pHashUtil.hammingDistance(0L, -1L)); // -1 = 64 位全 1
    }

    @Test
    @DisplayName("aHash/dHash 可用且同图自洽（1.3 实验前置）")
    void aHashDHashAvailable() throws Exception {
        byte[] b = toBytes(makeImage(5, 1.0f));
        assertEquals(pHashUtil.hashAverage(b), pHashUtil.hashAverage(b));
        assertEquals(pHashUtil.hashDifference(b), pHashUtil.hashDifference(b));
    }
}
