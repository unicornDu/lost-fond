package com.shiguang.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交付文件表（桌面端 P1，dsh"业务只声明→持久事件→UI 读表渲染"转译）。
 * file_path 只存相对 uploads 根的路径——下载/打开由服务端按 id 读回，
 * 浏览器/渲染层永远不传路径（dsh 踩坑记录第 1 条：浏览器传路径=任意文件读取漏洞）。
 */
@Data
@TableName("delivery")
public class Delivery {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** CLAIM_CERT 认领证明 / REPORT_CSV 统计报表 / MATCH_REPORT 匹配报告 */
    private String bizType;
    /** 关联业务 ID（item；启事删除时级联清理） */
    private Long bizId;
    private String title;
    private String description;
    private String filePath;
    private String fileName;
    private String mimeType;
    private Long fileSize;
    /** 1有效 0失效 */
    private Integer status;
    private LocalDateTime createTime;
}
