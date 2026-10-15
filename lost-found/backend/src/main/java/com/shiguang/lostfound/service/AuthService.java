package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.config.JwtProperties;
import com.shiguang.lostfound.dto.LoginReq;
import com.shiguang.lostfound.dto.RegisterReq;
import com.shiguang.lostfound.entity.User;
import com.shiguang.lostfound.mapper.UserMapper;
import com.shiguang.lostfound.util.JwtUtil;
import com.shiguang.lostfound.vo.LoginVO;
import com.shiguang.lostfound.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * M1 用户模块：注册 / 登录（JWT）。
 * 审查 2.29①：登录失败限流——同账号连错 5 次锁定 10 分钟（内存计数，无需 Redis）。
 * 审查 2.40：记住我 → 签发更长有效期（remember-minutes），否则 expire-minutes。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProps;

    private static final List<String> AVATARS = List.of("🦊", "🐰", "🐼", "🐯", "🦁", "🐨", "🐷", "🐸");
    private static final int MAX_FAIL = 5;
    private static final long LOCK_MS = 10 * 60 * 1000L;

    /** 账号 → [失败次数, 锁定截止时间]。 */
    private final ConcurrentHashMap<String, long[]> loginFail = new ConcurrentHashMap<>();

    public LoginVO register(RegisterReq req) {
        Long exists = userMapper.selectCount(new QueryWrapper<User>().eq("account", req.getAccount()));
        if (exists != null && exists > 0) throw new BusinessException("该学号已注册，请直接登录");

        User u = new User();
        u.setAccount(req.getAccount());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        String nick = (req.getNickname() == null || req.getNickname().isBlank())
                ? "同学" + req.getAccount().substring(req.getAccount().length() - 4)
                : req.getNickname().trim();
        u.setNickname(nick);
        u.setAvatar(AVATARS.get((int) (Math.random() * AVATARS.size())));
        u.setRole(0);
        u.setStatus(1);
        userMapper.insert(u);
        return buildLogin(u, false);
    }

    public LoginVO login(LoginReq req) {
        String acc = req.getAccount().trim();
        // 审查 2.29① 限流
        long[] st = loginFail.get(acc);
        if (st != null && st[1] > System.currentTimeMillis())
            throw new BusinessException("错误次数过多，账号已临时锁定，请 10 分钟后再试");

        User u = userMapper.selectOne(new QueryWrapper<User>().eq("account", acc));
        if (u == null || !passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            long[] cur = loginFail.computeIfAbsent(acc, k -> new long[]{0, 0});
            cur[0]++;
            if (cur[0] >= MAX_FAIL) cur[1] = System.currentTimeMillis() + LOCK_MS;
            throw new BusinessException("学号或密码错误");
        }
        if (u.getStatus() != null && u.getStatus() == 0) throw new BusinessException("账号已被封禁，请联系管理员");
        loginFail.remove(acc);
        return buildLogin(u, req.isRememberMe());
    }

    private LoginVO buildLogin(User u, boolean rememberMe) {
        long ttl = rememberMe ? jwtProps.getRememberMinutes() : jwtProps.getExpireMinutes();
        String token = jwtUtil.generate(u.getId(), u.getAccount(), u.getRole(), ttl);
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setAccount(u.getAccount());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setRole(u.getRole());
        return LoginVO.of(token, vo);
    }
}
