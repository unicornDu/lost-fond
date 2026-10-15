package com.shiguang.lostfound.vo;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String account;
    private String nickname;
    private String avatar;
    private Integer role;
    // 审查 2.8/附录E1：后台用户管理需要状态与注册时间
    private Integer status;      // 1正常 0封禁
    private String createTime;   // yyyy-MM-dd HH:mm
}
