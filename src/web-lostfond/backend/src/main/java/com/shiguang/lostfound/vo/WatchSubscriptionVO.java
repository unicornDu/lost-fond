package com.shiguang.lostfound.vo;

import lombok.Data;

/** v2.20 当前订阅状态（含方案权益，前端直接渲染「当前方案」与到期时间）。 */
@Data
public class WatchSubscriptionVO {
    private String planCode;
    private String planName;
    private String cycle;
    private Integer status;
    /** yyyy-MM-dd HH:mm */
    private String startTime;
    /** yyyy-MM-dd HH:mm；null=长期有效 */
    private String expireTime;
    private int quota;
    private int threshold;
    private int retentionDays;
    private int price;
    private int termPrice;
}
