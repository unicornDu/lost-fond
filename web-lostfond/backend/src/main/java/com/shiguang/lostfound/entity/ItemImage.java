package com.shiguang.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 启事图片 + 64 位感知哈希（以图搜图核心表）。
 */
@Data
@TableName("item_image")
public class ItemImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long itemId;
    private String imgUrl;
    private Long phash;   // 有符号 long 存 64 位指纹
    private Integer sort;
    private LocalDateTime createTime;
}
