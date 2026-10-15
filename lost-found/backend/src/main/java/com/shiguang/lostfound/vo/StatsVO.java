package com.shiguang.lostfound.vo;

import lombok.Data;

@Data
public class StatsVO {
    private long reunited;   // 已找回/已认领
    private long lostCount;  // 寻物启事数
    private long foundCount; // 招领启事数
}
