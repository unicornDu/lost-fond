package com.shiguang.lostfound.dto;

import lombok.Data;

@Data
public class ClaimAuditReq {
    /** true=通过认领，false=拒绝 */
    private boolean approve;
    private String reply;
}
