package com.shiguang.lostfound.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * E-01 资料编辑表单。字段均可选：null = 不修改该项（部分更新）。
 * 长度对齐注册侧与 DB 列宽：nickname ≤12（RegisterReq 同款）、phone ≤20（user.phone VARCHAR(20)）、
 * avatar ≤16（user.avatar VARCHAR(16)，前端从预设 emoji 挑选，方案 A）。
 */
@Data
public class UpdateProfileReq {

    @Size(max = 12, message = "昵称最多 12 个字")
    private String nickname;

    @Size(max = 20, message = "手机号最多 20 位")
    private String phone;

    @Size(max = 16, message = "头像标识过长")
    private String avatar;
}
