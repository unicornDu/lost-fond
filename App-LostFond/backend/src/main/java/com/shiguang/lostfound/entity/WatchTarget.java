package com.shiguang.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * v2.20 守望位：一张「希望被一直盯着」的照片。
 * 新招领通过审核时，WatchService 用本表 phash 与 item_image.phash 求汉明距离，
 * 命中则给守望人写一条 type=watch 的站内通知。
 */
@Data
@TableName("watch_target")
public class WatchTarget {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 64 位感知哈希（有符号 BIGINT，与 item_image.phash 同口径） */
    private Long phash;
    private String label;
    private String imgUrl;
    private Long sourceItemId;
    /** 1 守望中 0 已结束 */
    private Integer status;
    private Integer hitCount;
    private Long lastHitItemId;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
