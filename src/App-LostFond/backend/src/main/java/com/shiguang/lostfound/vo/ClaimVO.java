package com.shiguang.lostfound.vo;

import lombok.Data;

/**
 * 「我的认领」列表项：启事信息 + 认领状态。
 */
@Data
public class ClaimVO {
    private Long claimId;
    private Long id;          // 启事ID（前端沿用 item.id 跳转）
    private String title;
    private String img;
    private String location;
    private String time;
    private String claimStatus; // 审核中/已通过/已拒绝
    private String reply;
    // 审查 2.5：发布者审核需看到"谁申请、凭证、怎么联系"，杜绝盲审
    private String claimantName;    // 申请人昵称
    private String claimantAccount; // 申请人学号
    private String proof;           // 认领凭证
    private String contact;         // 联系方式
}
