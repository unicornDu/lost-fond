package com.shiguang.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修复 B-10：私信入参从裸 Map 改为带校验的 DTO。
 * content 上限 200——服务端还要拼"昵称 就「标题」给你留言："前缀，
 * message.content 列宽 VARCHAR(300)，200 + 前缀留有余量；sendDm 内再对总长兜底校验。
 */
@Data
public class DmReq {
    @NotBlank(message = "私信内容不能为空")
    @Size(max = 200, message = "私信最多 200 字")
    private String content;
}
