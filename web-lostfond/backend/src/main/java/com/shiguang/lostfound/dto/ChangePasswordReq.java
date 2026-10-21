package com.shiguang.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * E-01 修改密码表单。新密码规则与注册侧 RegisterReq 对齐（6-32 位）；
 * 旧密码在服务层 BCrypt match 校验（match 失败与"为空"都归 400，不区分泄露信息）。
 */
@Data
public class ChangePasswordReq {

    @NotBlank(message = "请输入原密码")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "新密码至少 6 位")
    private String newPassword;
}
