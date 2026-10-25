package com.shiguang.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** E-07：启事收藏。uk_user_item 唯一索引保证幂等。 */
@Data
@TableName("favorite")
public class Favorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long itemId;
    private LocalDateTime createTime;
}
