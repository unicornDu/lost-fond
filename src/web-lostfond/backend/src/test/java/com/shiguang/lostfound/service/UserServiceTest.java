package com.shiguang.lostfound.service;

import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.dto.ChangePasswordReq;
import com.shiguang.lostfound.dto.UpdateProfileReq;
import com.shiguang.lostfound.entity.User;
import com.shiguang.lostfound.mapper.UserMapper;
import com.shiguang.lostfound.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * E-01：账号自助管理单测。改密 = 旧密码 BCrypt match + 新密码 BCrypt 入库；
 * 资料编辑 = 部分更新（null 不动）+ 读回一致；昵称空串拒绝；不存在的用户 401。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ItemService itemService;   // E-04：公开主页 itemCount 复用 ItemService 口径

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userMapper, passwordEncoder, itemService);
    }

    private User user() {
        User u = new User();
        u.setId(5L);
        u.setAccount("20240005");
        u.setPassword("bcrypt-old");
        u.setNickname("旧昵称");
        u.setAvatar("🦊");
        u.setRole(0);
        u.setStatus(1);
        return u;
    }

    // ---------------- changePassword ----------------

    @Test
    @DisplayName("changePassword：旧密码不匹配 → 400 且不落库")
    void changePasswordWrongOld() {
        when(userMapper.selectById(5L)).thenReturn(user());
        when(passwordEncoder.matches("wrong", "bcrypt-old")).thenReturn(false);
        assertEquals("原密码不正确", assertThrows(BusinessException.class,
                () -> service.changePassword(5L, req("wrong", "newpass1"))).getMessage());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("changePassword：正常改密 → BCrypt 编码新密码并入库")
    void changePasswordOk() {
        when(userMapper.selectById(5L)).thenReturn(user());
        when(passwordEncoder.matches("oldpass", "bcrypt-old")).thenReturn(true);
        when(passwordEncoder.encode("newpass1")).thenReturn("bcrypt-new");
        assertDoesNotThrow(() -> service.changePassword(5L, req("oldpass", "newpass1")));
        verify(userMapper).updateById(argThat((User u) -> "bcrypt-new".equals(u.getPassword())));
    }

    @Test
    @DisplayName("changePassword：用户不存在 → 401")
    void changePasswordNoUser() {
        when(userMapper.selectById(99L)).thenReturn(null);
        assertEquals(401, assertThrows(BusinessException.class,
                () -> service.changePassword(99L, req("oldpass", "newpass1"))).getCode());
    }

    // ---------------- updateProfile ----------------

    @Test
    @DisplayName("updateProfile：全部字段读回一致，VO 不含密码")
    void profileUpdatesAll() {
        when(userMapper.selectById(5L)).thenReturn(user());
        UpdateProfileReq r = new UpdateProfileReq();
        r.setNickname("新昵称");
        r.setPhone("13800000000");
        r.setAvatar("🐼");
        UserVO vo = service.updateProfile(5L, r);
        assertEquals("新昵称", vo.getNickname());
        assertEquals("🐼", vo.getAvatar());
        verify(userMapper).updateById(argThat((User u) ->
                "新昵称".equals(u.getNickname()) && "13800000000".equals(u.getPhone()) && "🐼".equals(u.getAvatar())));
    }

    @Test
    @DisplayName("updateProfile：null 字段不动（部分更新）")
    void profilePartialUpdate() {
        User u = user();
        when(userMapper.selectById(5L)).thenReturn(u);
        UpdateProfileReq r = new UpdateProfileReq();
        r.setAvatar("🐨");
        service.updateProfile(5L, r);
        assertEquals("旧昵称", u.getNickname());
        assertNull(u.getPhone());
        assertEquals("🐨", u.getAvatar());
    }

    @Test
    @DisplayName("updateProfile：昵称纯空白 → 400")
    void profileBlankNickname() {
        when(userMapper.selectById(5L)).thenReturn(user());
        UpdateProfileReq r = new UpdateProfileReq();
        r.setNickname("   ");
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.updateProfile(5L, r)).getCode());
    }

    // ---------------- E-04：公开主页（脱敏） ----------------

    @Test
    @DisplayName("publicProfile：返回脱敏字段（无 account），itemCount 走公开口径")
    void publicProfileDesensitized() {
        User u = user();
        u.setCreateTime(java.time.LocalDateTime.of(2026, 1, 1, 10, 0));
        when(userMapper.selectById(5L)).thenReturn(u);
        when(itemService.publicItemCount(5L)).thenReturn(3L);
        var m = service.publicProfile(5L);
        assertFalse(m.containsKey("account"));
        assertEquals("旧昵称", m.get("nickname"));
        assertEquals(3L, m.get("itemCount"));
        verify(itemService).publicItemCount(5L);
    }

    @Test
    @DisplayName("publicProfile：封禁用户 → 404（不暴露封禁名单）")
    void publicProfileBanned404() {
        User u = user();
        u.setStatus(0);
        when(userMapper.selectById(5L)).thenReturn(u);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.publicProfile(5L)).getCode());
    }

    private ChangePasswordReq req(String oldPwd, String newPwd) {
        ChangePasswordReq r = new ChangePasswordReq();
        r.setOldPassword(oldPwd);
        r.setNewPassword(newPwd);
        return r;
    }
}
