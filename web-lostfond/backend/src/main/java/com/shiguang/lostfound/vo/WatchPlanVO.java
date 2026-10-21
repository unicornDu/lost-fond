package com.shiguang.lostfound.vo;

import lombok.Data;

/**
 * v2.20 守望方案（价格单位为元；演示环境不扣费）。
 * 名称/文案由前端按 code 走 i18n，这里只下发数值权益，避免前后端各写一套。
 */
@Data
public class WatchPlanVO {
    private String code;
    /** 中文名，供后端提示语与无 i18n 场景兜底 */
    private String name;
    /** 月付价格 */
    private int price;
    /** 学期价格 */
    private int termPrice;
    /** 守望位数量 */
    private int quota;
    /** 命中判定阈值（64 位汉明距离，越小越严） */
    private int threshold;
    /** 守望位保留天数 */
    private int retentionDays;
    /** 是否热门档 */
    private boolean popular;
}
