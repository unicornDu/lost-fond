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

    // ---------- 修复 B-04：登录限流锁定期过后必须重置计数 ----------

    /** 把 loginFail 里某账号的锁定截止时间改到过去，模拟"锁定期已过"。 */
    @SuppressWarnings("unchecked")
    private void expireLock(String account) {
        java.util.concurrent.ConcurrentHashMap<String, long[]> map =
                (java.util.concurrent.ConcurrentHashMap<String, long[]>)
                        org.springframework.test.util.ReflectionTestUtils.getField(authService, "loginFail");
        long[] st = map.get(account);
        assertNotNull(st, "限流状态应存在");
        st[1] = System.currentTimeMillis() - 1;
    }

    @Test
    @DisplayName("修复B-04：连错 5 次后第 6 次提示锁定")
    void loginLocksAfterFiveFails() {
        User u = new User(); u.setId(1L); u.setAccount("20260006"); u.setPassword("hash"); u.setStatus(1);
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(u);
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        LoginReq req = new LoginReq(); req.setAccount("20260006"); req.setPassword("bad");
        for (int i = 0; i < 5; i++)
            assertEquals("学号或密码错误", assertThrows(BusinessException.class, () -> authService.login(req)).getMessage());
        assertTrue(assertThrows(BusinessException.class, () -> authService.login(req))
                .getMessage().contains("临时锁定"));
    }

    @Test
    @DisplayName("修复B-04：锁定期过后再错 1 次是普通错误，不立刻二次锁定")
    void loginLockExpiryResetsCounter() {
        User u = new User(); u.setId(1L); u.setAccount("20260007"); u.setPassword("hash"); u.setStatus(1);
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(u);
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        LoginReq req = new LoginReq(); req.setAccount("20260007"); req.setPassword("bad");
        for (int i = 0; i < 5; i++) assertThrows(BusinessException.class, () -> authService.login(req));
        assertTrue(assertThrows(BusinessException.class, () -> authService.login(req))
                .getMessage().contains("临时锁定"));
        expireLock("20260007");
        // 锁定期过：再错 2 次都应是普通"学号或密码错误"（修复前第 1 次就会再次锁定）
        for (int i = 0; i < 2; i++)
            assertEquals("学号或密码错误", assertThrows(BusinessException.class, () -> authService.login(req)).getMessage());
    }

    @Test
    @DisplayName("修复B-04：登录成功清零失败计数")
    void loginSuccessClearsFailCounter() {
        User u = new User(); u.setId(1L); u.setAccount("20260008"); u.setPassword("hash"); u.setStatus(1); u.setRole(0);
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(u);
        when(passwordEncoder.matches(any(), any())).thenReturn(false, false, true,
                false, false, false, false);
        when(jwtUtil.generate(anyLong(), any(), any(), org.mockito.ArgumentMatchers.anyLong())).thenReturn("tok");
        LoginReq bad = new LoginReq(); bad.setAccount("20260008"); bad.setPassword("bad");
        LoginReq good = new LoginReq(); good.setAccount("20260008"); good.setPassword("ok");
        assertThrows(BusinessException.class, () -> authService.login(bad));
        assertThrows(BusinessException.class, () -> authService.login(bad));
        authService.login(good);                       // 成功 → 计数清零
        // 再连错 4 次（累计若没清零将达 6 次）都不应触发锁定
        for (int i = 0; i < 4; i++)
            assertEquals("学号或密码错误", assertThrows(BusinessException.class, () -> authService.login(bad)).getMessage());
    }
}
