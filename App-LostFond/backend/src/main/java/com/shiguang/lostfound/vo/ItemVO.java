package com.shiguang.lostfound.vo;

import lombok.Data;

import java.util.List;

/**
 * 启事展示对象。字段与前端 mock 契约保持一致：
 * id/type/title/category/location/time/status/owner/views/desc/img。
 * similarity 仅以图搜图结果返回（其余场景为 null）。
 */
@Data
public class ItemVO {
    private Long id;
    private String type;
    private String title;
    private String category;
    private String location;
    /** v2.41.1：精确坐标（GCJ-02）；为 null 表示发布者没在地图上标点。 */
    private Double latitude;
    private Double longitude;
    private String time;        // 格式化后的发生时间 yyyy-MM-dd HH:mm
    private String status;
    private String owner;       // 发布人昵称
    private Integer views;
    private String desc;        // 详细描述
    private String img;         // 封面原图相对路径
    private String thumb;       // 缩略图相对路径（列表用；缺失时前端回退 img）审查 2.17
    private Integer auditStatus;// 审核状态 0待审核 1已通过 2已驳回（后台用）审查 2.8
    private List<String> images; // 全部图片（详情页用）
    private Long userId;        // 发布人ID（前端判断是否本人）
    private Boolean mine;       // 是否当前登录用户发布
    private Double similarity;  // 以图搜图相似度百分比
    /** 2026-10-18 审查修复：当前登录用户对本启事的认领状态/回复（详情接口一次带回，非本人或未申请为 null）。 */
    private String myClaimStatus;
    private String myClaimReply;
}
