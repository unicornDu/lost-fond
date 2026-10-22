package com.shiguang.lostfound.util;

import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.config.ImageSearchProperties;
import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.AverageHash;
import dev.brachtendorf.jimagehash.hashAlgorithms.DifferenceHash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigInteger;

/**
 * 感知哈希工具 —— 本项目核心算法（M3）。
 * 主算法 pHash（PerceptiveHash）：缩放→灰度→DCT→取低频→二值化，64 位指纹。
 * 审查 1.3：另提供 aHash（AverageHash）/ dHash（DifferenceHash）用于准确率对比实验。
 * 底层库：dev.brachtendorf:JImageHash（Maven Central）。
 */
@Slf4j
@Component
public class PHashUtil {

    private final HashingAlgorithm hasher;      // pHash（生产用）
    private final HashingAlgorithm aHasher;     // aHash（实验对比用）
    private final HashingAlgorithm dHasher;     // dHash（实验对比用）
    private final int bitResolution;

    public PHashUtil(ImageSearchProperties props) {
        this.bitResolution = props.getBitResolution();
        this.hasher = new PerceptiveHash(bitResolution);
        this.aHasher = new AverageHash(bitResolution);
        this.dHasher = new DifferenceHash(bitResolution, DifferenceHash.Precision.Simple);
    }

    /** 计算图片字节的 64 位 pHash（存为有符号 long，汉明距离用 Long.bitCount(a^b)）。 */
    public long hash(byte[] imageBytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img == null) throw new BusinessException("无法解析图片内容，请上传 JPG/PNG 格式");
            return hash(img);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("计算 pHash 失败", e);
            throw new BusinessException("图片处理失败：" + e.getMessage());
        }
    }

    public long hash(File file) {
        try {
            Hash h = hasher.hash(file);
            return toLong(h.getHashValue());
        } catch (Exception e) {
            log.error("计算文件 pHash 失败: {}", file, e);
            throw new BusinessException("图片处理失败：" + e.getMessage());
        }
    }

    public long hash(BufferedImage img) {
        Hash h = hasher.hash(img);
        return toLong(h.getHashValue());
    }

    private long toLong(BigInteger v) {
        return v.longValue();
    }

    /** 汉明距离：两个 64 位指纹有多少位不同（0=完全一致）。 */
    public int hammingDistance(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    /** 相似度百分比：(1 - 距离/位数) * 100，保留一位小数。 */
    public double similarity(long a, long b) {
        int dist = hammingDistance(a, b);
        double sim = (1.0 - (double) dist / bitResolution) * 100.0;
        return Math.round(sim * 10.0) / 10.0;
    }

    /** 由汉明距离直接算相似度（实验/展示用）。 */
    public double similarityFromDist(int dist) {
        double sim = (1.0 - (double) dist / bitResolution) * 100.0;
        return Math.round(sim * 10.0) / 10.0;
    }

    // ---------- 审查 1.3：对比实验用 aHash / dHash ----------
    public long hashAverage(byte[] imageBytes) { return hashWith(aHasher, imageBytes); }
    public long hashDifference(byte[] imageBytes) { return hashWith(dHasher, imageBytes); }

    private long hashWith(HashingAlgorithm alg, byte[] imageBytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img == null) throw new BusinessException("无法解析图片内容");
            return alg.hash(img).getHashValue().longValue();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("图片处理失败：" + e.getMessage());
        }
    }
}
