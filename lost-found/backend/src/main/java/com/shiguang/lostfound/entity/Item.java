package com.shiguang.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("item")
public class Item {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;        // lost / found
    private String title;
    private String category;
    private String location;
    private LocalDateTime happenTime;
    private String description;
    private String status;      // 寻找中/待认领/认领申请中/已找回/已认领/已下架/已关闭
    private String coverImg;
    private Integer views;
    private Long userId;
    private Integer auditStatus; // 0待审核 1已通过 2已驳回
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
