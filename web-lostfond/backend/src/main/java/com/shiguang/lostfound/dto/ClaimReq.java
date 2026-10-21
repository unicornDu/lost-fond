package com.shiguang.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClaimReq {
    @NotBlank(message = "请填写认领凭证")
    @Size(max = 200, message = "认领凭证最多 200 字")
    private String proof;
    @Size(max = 50, message = "联系方式过长")
    private String contact;
}
