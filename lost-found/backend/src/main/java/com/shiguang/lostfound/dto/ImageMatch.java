package com.shiguang.lostfound.dto;

import lombok.Data;

/**
 * 以图搜图中间结果：某启事图片与查询图的最小汉明距离。
 */
@Data
public class ImageMatch {
    private Long itemId;
    private Integer dist;
}
