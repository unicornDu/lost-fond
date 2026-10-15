package com.shiguang.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("claim_record")
public class ClaimRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long itemId;
    private Long claimantId;
    private String proof;
    private String contact;
    private String status;   // 审核中/已通过/已拒绝
    private String reply;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
