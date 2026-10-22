package com.shiguang.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterReq {
    @NotBlank(message = "请输入学号")
    @Pattern(regexp = "^\\d{6,12}$", message = "学号需为 6-12 位数字")
    private String account;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 32, message = "密码至少 6 位")
    private String password;

    @Size(max = 12, message = "昵称最多 12 个字")
    private String nickname;
}
