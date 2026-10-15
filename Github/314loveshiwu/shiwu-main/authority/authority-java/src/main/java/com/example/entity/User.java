package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 *
 * </p>
 *
 * @author   
 * @since 2023-01-07
 */
@Getter
@Setter
@TableName("sys_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("avatar")
    private String avatar;

    @TableField("email")
    private String email;
    @TableField("phone")
    private String phone;

    @TableField("city")
    private String city;

    @TableField("created")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime created;

    @TableField("updated")
    private LocalDateTime updated;

    @TableField("last_login")
    private LocalDateTime lastLogin;

    @TableField("statu")
    private Integer statu;
    @TableField(exist = false)
    private List<Role> roles=new ArrayList<>();
    @TableField(exist = false)
    private List<Role>sysRoles=new ArrayList<>();

}
