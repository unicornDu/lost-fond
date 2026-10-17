package com.shiguang.lostfound.service;

import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.dto.ChangePasswordReq;
import com.shiguang.lostfound.dto.UpdateProfileReq;
import com.shiguang.lostfound.entity.User;
import com.shiguang.lostfound.mapper.UserMapper;
import com.shiguang.lostfound.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * E-01 账号自助管理：修改密码 / 资料编辑（昵称、头像、手机号）。
 * E-04 他人公开主页：profile 只返回脱敏字段——不返回 account（学号属登录凭证，
 * SRS 仅允许认领审核场景对发布者可见）；封禁用户与不存在用户一律 404（不暴露封禁名单）。
 * phone 由死字段转正：进入 profile 编辑口径（见 docs/06 user.phone 说明）。
 * 修改密码无服务端吊销：已签发 token 在有效期内仍可用（既定取舍，前端改密成功即强制重新登录）。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ItemService itemService;

    private static final DateTimeFormatter OUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void changePassword(Long userId, ChangePasswordReq req) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BusinessException(401, "请先登录");
        if (!passwordEncoder.matches(req.getOldPassword(), u.getPassword()))
            throw new BusinessException(400, "原密码不正确");
        u.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(u);
    }

    /** 部分更新：req 中为 null 的字段不修改。返回更新后的 VO（含 account/role，供前端刷新 store）。 */
    public UserVO updateProfile(Long userId, UpdateProfileReq req) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BusinessException(401, "请先登录");
        if (req.getNickname() != null) {
            String nick = req.getNickname().trim();
            if (nick.isEmpty()) throw new BusinessException(400, "昵称不能为空");
            u.setNickname(nick);
        }
        if (req.getPhone() != null) {
            String phone = req.getPhone().trim();
            u.setPhone(phone.isEmpty() ? null : phone);   // 空串 = 清空手机号（回写 NULL，与列默认一致）
        }
        if (req.getAvatar() != null && !req.getAvatar().isBlank()) u.setAvatar(req.getAvatar().trim());
        userMapper.updateById(u);
        return toVO(u);
    }

    private UserVO toVO(User u) {
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setAccount(u.getAccount());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setPhone(u.getPhone());
        vo.setRole(u.getRole());
        return vo;
    }

    // ---------------- E-04：他人公开主页 ----------------

    /** 公开资料（脱敏）：无 account；封禁/不存在 → 404。itemCount 复用 ItemService 公开口径。 */
    public Map<String, Object> publicProfile(Long userId) {
        User u = visibleUser(userId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("nickname", u.getNickname());
        m.put("avatar", u.getAvatar());
        m.put("createTime", u.getCreateTime() == null ? "" : u.getCreateTime().format(OUT));
        m.put("itemCount", itemService.publicItemCount(userId));
        return m;
    }

    /** E-04 共用守卫：用户不存在或已封禁 → 404（封禁用户不渲染主页、不进列表口径）。 */
    private User visibleUser(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null || (u.getStatus() != null && u.getStatus() == 0))
            throw new BusinessException(404, "用户不存在");
        return u;
    }
}
