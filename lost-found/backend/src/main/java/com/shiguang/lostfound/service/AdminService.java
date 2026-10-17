package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.common.PageResult;
import com.shiguang.lostfound.entity.*;
import com.shiguang.lostfound.mapper.*;
import com.shiguang.lostfound.vo.ItemVO;
import com.shiguang.lostfound.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * M6 管理后台。所有操作写 admin_log。
 * 审查 2.8：listUsers 回填 status/createTime；新增 listLogs（2.29③）、regenThumbs（2.17）。
 * 审查 2.20：deleteItem 同步清理磁盘图片与关联 claim/message，杜绝孤儿文件。
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final ClaimRecordMapper claimMapper;
    private final MessageMapper messageMapper;
    private final AdminLogMapper adminLogMapper;
    private final ItemService itemService;
    private final FileStorageService fileStorage;

    private static final DateTimeFormatter OUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public PageResult<UserVO> listUsers(long page, long size, String keyword) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        QueryWrapper<User> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank())
            qw.and(w -> w.like("account", keyword).or().like("nickname", keyword));
        qw.orderByDesc("create_time");
        Page<User> p = userMapper.selectPage(new Page<>(page, size), qw);
        List<UserVO> records = p.getRecords().stream().map(u -> {
            UserVO v = new UserVO();
            v.setId(u.getId()); v.setAccount(u.getAccount());
            v.setNickname(u.getNickname()); v.setAvatar(u.getAvatar()); v.setRole(u.getRole());
            v.setStatus(u.getStatus());                                   // 审查 2.8
            v.setCreateTime(u.getCreateTime() == null ? "" : u.getCreateTime().format(OUT));
            return v;
        }).collect(Collectors.toList());
        return PageResult.of(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Transactional
    public void setUserStatus(Long adminId, Long userId, boolean ban) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BusinessException(404, "用户不存在");
        if (u.getRole() != null && u.getRole() == 1) throw new BusinessException("不能封禁管理员");
        u.setStatus(ban ? 0 : 1);
        userMapper.updateById(u);
        log(adminId, ban ? "ban" : "unban", "user", userId, "封禁状态=" + (ban ? 0 : 1));
    }

    public PageResult<ItemVO> listItems(long page, long size, Integer auditStatus, String type) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        QueryWrapper<Item> qw = new QueryWrapper<>();
        if (auditStatus != null) qw.eq("audit_status", auditStatus);
        if (type != null && !type.isBlank()) qw.eq("type", type);
        qw.orderByDesc("create_time");
        Page<Item> p = itemMapper.selectPage(new Page<>(page, size), qw);
        var nick = itemService.loadNicknames(p.getRecords().stream()
                .map(Item::getUserId).collect(Collectors.toSet()));
        List<ItemVO> records = p.getRecords().stream()
                .map(it -> itemService.toVO(it, nick, null)).collect(Collectors.toList());
        return PageResult.of(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Transactional
    public void auditItem(Long adminId, Long itemId, boolean approve) {
        Item it = itemMapper.selectById(itemId);
        if (it == null) throw new BusinessException(404, "启事不存在");
        // 修复 B-03：业务已完成（已认领/已找回）的启事不允许再被审核改状态，避免把已团聚数据改坏
        if ("已认领".equals(it.getStatus()) || "已找回".equals(it.getStatus()))
            throw new BusinessException("该启事已完成认领流程，不允许再审核修改");
        it.setAuditStatus(approve ? 1 : 2);
        // 修复 B-03：审核维度与业务维度解耦——驳回只改 audit_status，不再写「已关闭」；
        // 通过时若历史驳回曾把 status 写成「已关闭」，恢复业务初始状态，否则通过后前台永远看不到
        if (approve && "已关闭".equals(it.getStatus()))
            it.setStatus("lost".equals(it.getType()) ? "寻找中" : "待认领");
        itemMapper.updateById(it);
        log(adminId, "audit", "item", itemId, "审核=" + (approve ? "通过" : "驳回"));
    }

    @Transactional
    public void offlineItem(Long adminId, Long itemId) {
        Item it = itemMapper.selectById(itemId);
        if (it == null) throw new BusinessException(404, "启事不存在");
        // 修复 Z-10：与 auditItem 的 B-03 守卫对齐——已团聚（已认领/已找回）启事不允许下架改坏业务状态
        if ("已认领".equals(it.getStatus()) || "已找回".equals(it.getStatus()))
            throw new BusinessException("该启事已完成认领流程，不允许下架");
        it.setStatus("已下架");
        it.setAuditStatus(2);
        itemMapper.updateById(it);
        log(adminId, "offline", "item", itemId, "违规下架");
    }

    /** 审查 2.20：删除启事同时清理图片记录、磁盘文件、关联认领与消息。
     *  修复 B-11 取舍说明：先删库再删文件，删文件失败仅记日志——宁可留孤儿文件也不能丢库记录
     *  （库记录没了文件就是纯垃圾，反之文件还在尚可人工巡检）。 */
    @Transactional
    public void deleteItem(Long adminId, Long itemId) {
        Item it = itemMapper.selectById(itemId);
        if (it == null) throw new BusinessException(404, "启事不存在");
        List<ItemImage> imgs = itemImageMapper.selectList(
                new QueryWrapper<ItemImage>().eq("item_id", itemId));
        itemImageMapper.delete(new QueryWrapper<ItemImage>().eq("item_id", itemId));
        claimMapper.delete(new QueryWrapper<ClaimRecord>().eq("item_id", itemId));
        // 修复 B-05：不再限制 type——claim 与 dm（以及任何以 itemId 为 refId 的消息）一并清理，
        // 否则删除启事后私信仍留在收件人消息中心，点"查看启事"跳 404（悬挂数据）
        messageMapper.delete(new QueryWrapper<Message>().eq("ref_id", itemId));
        itemMapper.deleteById(itemId);
        for (ItemImage img : imgs) fileStorage.deleteQuietly(img.getImgUrl());
        log(adminId, "delete", "item", itemId, "删除启事（含图片与关联数据）");
    }

    /** 审查 2.29③/附录E4：操作日志分页读取。 */
    public PageResult<AdminLog> listLogs(long page, long size) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        Page<AdminLog> p = adminLogMapper.selectPage(new Page<>(page, size),
                new QueryWrapper<AdminLog>().orderByDesc("create_time"));
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 审查 2.17：为历史图片补生成缩略图。修复 B-08：写入范围收敛到用户上传目录 yyyy/MM，不碰 seed 等已提交目录。 */
    public int regenThumbs(Long adminId) {
        int n = fileStorage.regenUserUploadThumbs();
        log(adminId, "regen-thumbs", "item", null, "补生成缩略图 " + n + " 张");
        return n;
    }

    /** 05 A-01：六表行数（仅管理员）。供 test_api.py 幂等自检与运维核对——
     *  此前自检只有 stats/item_image 两个口径，message 与 admin_log 的残留永远测不出来。 */
    public Map<String, Long> dbCounts() {
        Map<String, Long> m = new java.util.LinkedHashMap<>();
        m.put("user", userMapper.selectCount(null));
        m.put("item", itemMapper.selectCount(null));
        m.put("item_image", itemImageMapper.selectCount(null));
        m.put("claim_record", claimMapper.selectCount(null));
        m.put("message", messageMapper.selectCount(null));
        m.put("admin_log", adminLogMapper.selectCount(null));
        return m;
    }

    private void log(Long adminId, String action, String targetType, Long targetId, String detail) {
        AdminLog l = new AdminLog();
        l.setAdminId(adminId);
        l.setAction(action);
        l.setTargetType(targetType);
        l.setTargetId(targetId);
        l.setDetail(detail);
        adminLogMapper.insert(l);
    }
}
