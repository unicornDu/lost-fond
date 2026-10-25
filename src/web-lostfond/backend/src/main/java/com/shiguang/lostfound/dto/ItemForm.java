package com.shiguang.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布启事表单。
 * 审查 2.28：补齐长度校验（与 DB 列宽一致）+ type 枚举校验，避免超长触发 500/数据异常。
 */
@Data
public class ItemForm {
    @NotBlank(message = "请选择类型")
    @Pattern(regexp = "^(lost|found)$", message = "类型只能是 lost 或 found")
    private String type;        // lost / found

    @NotBlank(message = "请填写物品名称")
    @Size(max = 60, message = "物品名称最多 60 字")
    private String title;

    @Size(max = 20, message = "分类最多 20 字")
    private String category;

    @Size(max = 50, message = "地点最多 50 字")
    private String location;

    /** v2.41.1：地图选点（GCJ-02）。两者都为空表示未标注精确位置；只填一个会被忽略。 */
    private Double latitude;
    private Double longitude;

    private String happenTime;  // yyyy-MM-dd HH:mm[:ss]

    @Size(max = 500, message = "描述最多 500 字")
    private String description;
}
