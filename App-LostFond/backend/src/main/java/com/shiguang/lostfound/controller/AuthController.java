package com.shiguang.lostfound.controller;

import com.shiguang.lostfound.common.Result;
import com.shiguang.lostfound.config.JwtProperties;
import com.shiguang.lostfound.context.UserContext;
import com.shiguang.lostfound.dto.LoginReq;
import com.shiguang.lostfound.dto.RegisterReq;
import com.shiguang.lostfound.entity.User;
import com.shiguang.lostfound.mapper.UserMapper;
import com.shiguang.lostfound.service.AuthService;
import com.shiguang.lostfound.util.JwtUtil;
import com.shiguang.lostfound.vo.LoginVO;
import com.shiguang.lostfound.vo.UserVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
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
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProps;

    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody @Valid RegisterReq req) {
        return Result.ok(authService.register(req));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginReq req) {
        return Result.ok(authService.login(req));
    }

    /**
     * 用令牌换取当前登录用户信息。修复 B-12：用户已被删除时返回 401 而非 NPE→500。
     * E-05：token 有效但用户已被封禁时（拦截器按匿名处理，uid 为空），这里单独解一次 token
     * 返回专用码 40301，供前端弹"账号已被封禁"提示；其余业务接口维持匿名口径不变（范围红线）。
     */
    @GetMapping("/me")
    public Result<UserVO> me(HttpServletRequest request) {
        Long uid = UserContext.getUserId();
        if (uid == null) {
            User banned = bannedUserOf(request);
            if (banned != null)
                throw new com.shiguang.lostfound.common.BusinessException(40301, "账号已被封禁，如有疑问请联系管理员");
            throw new com.shiguang.lostfound.common.BusinessException(401, "请先登录");
        }
        User u = userMapper.selectById(uid);
        if (u == null) throw new com.shiguang.lostfound.common.BusinessException(401, "请先登录");
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setAccount(u.getAccount());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setPhone(u.getPhone());   // E-01：本人资料回显
        vo.setRole(u.getRole());
        return Result.ok(vo);
    }

    /** E-05：请求带了有效 token、且对应用户 status=0（被封禁）→ 返回该用户；否则 null。 */
    private User bannedUserOf(HttpServletRequest request) {
        String header = request.getHeader(jwtProps.getHeader());
        if (header == null || !header.startsWith(jwtProps.getPrefix())) return null;
        Claims claims = jwtUtil.parse(header.substring(jwtProps.getPrefix().length()).trim());
        if (claims == null) return null;
        User u = userMapper.selectById(Long.valueOf(claims.getSubject()));
        return (u != null && u.getStatus() != null && u.getStatus() == 0) ? u : null;
    }
}
