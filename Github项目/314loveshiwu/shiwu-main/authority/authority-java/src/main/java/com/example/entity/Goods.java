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
 * 失物管理
 * </p>
 *
 * @author ren
 * @since 2023-02-13
 */
@Getter
@Setter
@TableName("goods")
public class Goods implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 序列号
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 失物名称
     */
    @TableField("name")
        private String name;

    /**
     * 失物图片
     */
    @TableField("img")
    private String img;

    /**
     * 拾取地点
     */
    @TableField("place")
    private String place;

    /**
     * 失物详细说明
     */
    @TableField("des")
    private String des;

    /**
     * 拾取日期
     */
    @TableField("date")
    private String date;

    /**
     * 联系电话
     */
    @TableField("phone")
    private String phone;


}
