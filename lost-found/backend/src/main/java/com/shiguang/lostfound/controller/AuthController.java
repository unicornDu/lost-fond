package com.shiguang.lostfound.controller;

import com.shiguang.lostfound.common.Result;
import com.shiguang.lostfound.context.UserContext;
import com.shiguang.lostfound.dto.LoginReq;
import com.shiguang.lostfound.dto.RegisterReq;
import com.shiguang.lostfound.entity.User;
import com.shiguang.lostfound.mapper.UserMapper;
import com.shiguang.lostfound.service.AuthService;
import com.shiguang.lostfound.vo.LoginVO;
import com.shiguang.lostfound.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * M1 用户模块：注册 / 登录 / 当前用户。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;

    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody @Valid RegisterReq req) {
        return Result.ok(authService.register(req));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginReq req) {
        return Result.ok(authService.login(req));
    }

    /** 用令牌换取当前登录用户信息。修复 B-12：用户已被删除时返回 401 而非 NPE→500。 */
    @GetMapping("/me")
    public Result<UserVO> me() {
        Long uid = UserContext.requireUserId();
        User u = userMapper.selectById(uid);
        if (u == null) throw new com.shiguang.lostfound.common.BusinessException(401, "请先登录");
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setAccount(u.getAccount());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setRole(u.getRole());
        return Result.ok(vo);
    }
}
