package com.shiguang.lostfound.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "image-search")
public class ImageSearchProperties {
    /** pHash 位分辨率，64 → 64 位指纹 */
    private int bitResolution = 64;
    /** 汉明距离阈值：≤ 此值判定为相似 */
    private int hammingThreshold = 10;
    /** 返回 Top-N */
    private int topN = 6;
}
