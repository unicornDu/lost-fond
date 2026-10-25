package com.shiguang.lostfound.vo;

import lombok.Data;

import java.util.List;

/** v2.20 守望订阅总览：当前方案 + 用量 + 守望位列表。 */
@Data
public class WatchOverviewVO {
    private WatchSubscriptionVO plan;
    private int quota;
    private int used;
    private List<WatchTargetVO> targets;
}
