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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 审查 2.15：AuthService 单元测试（Mockito，不连数据库）。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("unit-test-secret-unit-test-secret-unit-test");
        props.setExpireMinutes(1440);
        props.setRememberMinutes(43200);
        authService = new AuthService(userMapper, passwordEncoder, jwtUtil, props);
    }

    @Test
    @DisplayName("重复学号注册被拒")
    void registerDuplicateRejected() {
        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        RegisterReq req = new RegisterReq();
        req.setAccount("20260001");
        req.setPassword("Test@2026x");
        assertThrows(BusinessException.class, () -> authService.register(req));
    }

    @Test
    @DisplayName("注册成功返回 token")
    void registerSuccess() {
        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenReturn(1);
        // 注意：mock 的 insert 不会回填自增 id，故 id 用 any()（可为 null）
        when(jwtUtil.generate(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong())).thenReturn("tok");
        RegisterReq req = new RegisterReq();
        req.setAccount("20260002");
        req.setPassword("Test@2026x");
        req.setNickname("测试");
        LoginVO vo = authService.register(req);
        assertEquals("tok", vo.getToken());
        assertEquals("测试", vo.getUser().getNickname());
    }

    @Test
    @DisplayName("错误密码登录被拒")
    void loginWrongPassword() {
        User u = new User(); u.setId(1L); u.setAccount("20260003"); u.setPassword("hash"); u.setStatus(1);
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(u);
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        LoginReq req = new LoginReq(); req.setAccount("20260003"); req.setPassword("bad");
        assertThrows(BusinessException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("封禁账号登录被拒")
    void loginBanned() {
        User u = new User(); u.setId(1L); u.setAccount("20260004"); u.setPassword("hash"); u.setStatus(0);
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(u);
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        LoginReq req = new LoginReq(); req.setAccount("20260004"); req.setPassword("ok");
        assertThrows(BusinessException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("记住我使用更长 TTL")
    void loginRememberMeTtl() {
        User u = new User(); u.setId(1L); u.setAccount("20260005"); u.setPassword("hash"); u.setStatus(1); u.setRole(0);
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(u);
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtUtil.generate(anyLong(), any(), any(), org.mockito.ArgumentMatchers.anyLong())).thenReturn("tok");
        LoginReq req = new LoginReq(); req.setAccount("20260005"); req.setPassword("ok"); req.setRememberMe(true);
        authService.login(req);
        org.mockito.Mockito.verify(jwtUtil).generate(1L, "20260005", 0, 43200L);
    }
}
