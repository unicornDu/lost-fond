package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 失物求助管理
 * </p>
 *
 * @author ren
 * @since 2023-02-13
 */
@Getter
@Setter
@TableName("help")
public class Help implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 序列号
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 丢失物品名称
     */
    @TableField("name")
    private String name;

    /**
     * 物品图片
     */
    @TableField("img")
    private String img;

    /**
     * 丢失日期
     */
    @TableField("date")
    private String date;

    /**
     * 丢失地点
     */
    @TableField("place")
    private String place;

    /**
     * 联系电话
     */
    @TableField("phone")
    private String phone;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;


}
