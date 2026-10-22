package com.shiguang.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * v2.20 守望订阅。一人一条（uk_user），方案权益见 WatchService.PLANS。
 * 课程演示环境不接真实支付：开通只落库记录方案，不做扣费。
 */
@Data
@TableName("watch_subscription")
public class WatchSubscription {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** light / steady / crowd */
    private String planCode;
    /** month / semester */
    private String cycle;
    /** 1 生效 0 已取消 */
    private Integer status;
    private LocalDateTime startTime;
    /** NULL = 长期有效（轻守） */
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
