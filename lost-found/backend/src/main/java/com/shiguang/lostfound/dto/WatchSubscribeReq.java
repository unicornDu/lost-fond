package com.shiguang.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** v2.20 开通 / 切换守望方案。 */
@Data
public class WatchSubscribeReq {
    @NotBlank(message = "请选择守望方案")
    private String planCode;
    /** month / semester；不传按 month */
    private String cycle;
}
