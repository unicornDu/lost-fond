package com.shiguang.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginReq {
    @NotBlank(message = "请输入学号")
    private String account;
    @NotBlank(message = "请输入密码")
    private String password;
    /** 审查 2.40：记住我 → 更长 token 有效期（30 天），否则 1 天。 */
    private boolean rememberMe;
}
