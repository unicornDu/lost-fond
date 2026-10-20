package com.shiguang.lostfound.vo;

import lombok.Data;

/** v2.20 守望位（列表项）。 */
@Data
public class WatchTargetVO {
    private Long id;
    private String label;
    private String imgUrl;
    private Integer hitCount;
    private Long lastHitItemId;
    /** 最近命中启事的标题（列表展示「已命中《xx》」） */
    private String lastHitTitle;
    /** 剩余天数；null = 长期守望 */
    private Integer remainingDays;
    private String createTime;
    private String expireTime;
}
