package com.shiguang.lostfound.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClaimAuditReq {
    /** true=通过认领，false=拒绝 */
    private boolean approve;
    /** 修复 B-10：claim_record.reply 列宽 VARCHAR(200)，补长度校验（此前缺 @Size 且控制器缺 @Valid） */
    @Size(max = 200, message = "回复最多 200 字")
    private String reply;
}
