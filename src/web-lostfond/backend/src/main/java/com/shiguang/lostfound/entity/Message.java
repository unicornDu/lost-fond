package com.shiguang.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息。E-03：新增 from_user_id（发送者，NULL=系统消息），支持 dm 双向回复；
 * fromNickname/fromAvatar 为列表展示用的联表字段（不落库，MessageService 组装时回填）。
 */
@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** E-03：发送者 ID；NULL=系统消息（claim/audit/system），dm 恒有值 */
    private Long fromUserId;
    private String type;
    private String title;
    private String content;
    private Long refId;
    private Integer isRead;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String fromNickname;
    @TableField(exist = false)
    private String fromAvatar;
}
