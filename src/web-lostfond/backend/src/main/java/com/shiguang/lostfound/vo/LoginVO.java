package com.shiguang.lostfound.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private UserVO user;

    public static LoginVO of(String token, UserVO user) {
        LoginVO v = new LoginVO();
        v.token = token; v.user = user;
        return v;
    }
}
