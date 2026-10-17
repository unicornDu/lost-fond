package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.common.PageResult;
import com.shiguang.lostfound.entity.*;
import com.shiguang.lostfound.mapper.*;
import com.shiguang.lostfound.vo.ItemVO;
import com.shiguang.lostfound.vo.UserVO;import lombok.RequiredArgsConstructor;
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
    private final FavoriteService favoriteService;   // E-07：删除启事时级联清理收藏行
    private final FavoriteMapper favoriteMapper;
    private final FileDeliveryService fileDeliveryService;   // P1：交付体系（报表生成 + 级联清理）
    private final DeliveryMapper deliveryMapper;

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
    public void auditItem(Long adminId, Long itemId, boolean approve, String reason) {
        String r = checkedReason(reason);   // E-02：理由先校验，避免"超长理由已写了状态才发现 400"
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
        log(adminId, "audit", "item", itemId, "审核=" + (approve ? "通过" : "驳回") + logReason(r));
        // E-02：审核结果通知发布者（与 ClaimService.push 同模型口径：type=audit，refId=itemId）
        String content = approve
                ? "你的启事《" + it.getTitle() + "》已通过审核，现在可以在广场看到了"
                : "你的启事《" + it.getTitle() + "》未通过审核：" + (r != null ? "理由：" + r : "内容不符合发布规范");
        push(it.getUserId(), approve ? "审核通过" : "审核未通过", content, itemId);
    }

    @Transactional
    public void offlineItem(Long adminId, Long itemId, String reason) {
        String r = checkedReason(reason);
        Item it = itemMapper.selectById(itemId);
        if (it == null) throw new BusinessException(404, "启事不存在");
        // 修复 Z-10：与 auditItem 的 B-03 守卫对齐——已团聚（已认领/已找回）启事不允许下架改坏业务状态
        if ("已认领".equals(it.getStatus()) || "已找回".equals(it.getStatus()))
            throw new BusinessException("该启事已完成认领流程，不允许下架");
        it.setStatus("已下架");
        it.setAuditStatus(2);
        itemMapper.updateById(it);
        log(adminId, "offline", "item", itemId, "违规下架" + logReason(r));
        // E-02：下架结果通知发布者
        String content = "你的启事《" + it.getTitle() + "》已被管理员下架" + (r != null ? "：理由：" + r : "");
        push(it.getUserId(), "启事已下架", content, itemId);
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
        favoriteService.deleteByItem(itemId);   // E-07：级联清理收藏行
        fileDeliveryService.deleteByBiz(itemId);   // P1：级联清理交付文件（认领证明等，行+盘）
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

    /** 05 A-01：行数自检（仅管理员）。供 test_api.py 幂等自检与运维核对——
     *  E-07 起新增 favorite 表；favorite 与业务数据同走幂等基线。 */
    public Map<String, Long> dbCounts() {
        Map<String, Long> m = new java.util.LinkedHashMap<>();
        m.put("user", userMapper.selectCount(null));
        m.put("item", itemMapper.selectCount(null));
        m.put("item_image", itemImageMapper.selectCount(null));
        m.put("claim_record", claimMapper.selectCount(null));
        m.put("message", messageMapper.selectCount(null));
        m.put("favorite", favoriteMapper.selectCount(null));
        m.put("delivery", deliveryMapper.selectCount(null));
        m.put("admin_log", adminLogMapper.selectCount(null));
        return m;
    }

    // ---------------- P1：启事数据报表（CSV，入 delivery 体系） ----------------

    /** 生成报表交付卡片（REPORT_CSV）：走统一受控下载，替代原 /admin/export/items.csv 独立端点。 */
    @Transactional
    public Delivery createCsvReport(Long adminId) {
        List<Item> items = itemMapper.selectList(new QueryWrapper<Item>().orderByAsc("id"));
        Map<Long, String> nick = itemService.loadNicknames(items.stream()
                .map(Item::getUserId).collect(Collectors.toSet()));

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');   // UTF-8 BOM：Excel 直开不乱码
        sb.append("id,类型,名称,分类,地点,状态,审核状态,发布人,发布时间\n");
        for (Item it : items) {
            sb.append(String.join(",",
                    csv(it.getId()),
                    csv("lost".equals(it.getType()) ? "寻物启事" : "招领启事"),
                    csv(it.getTitle()),
                    csv(it.getCategory()),
                    csv(it.getLocation()),
                    csv(it.getStatus()),
                    csv(it.getAuditStatus() == null ? "" :
                            it.getAuditStatus() == 1 ? "已通过" : it.getAuditStatus() == 0 ? "待审核" : "已驳回"),
                    csv(nick.getOrDefault(it.getUserId(), "同学")),
                    csv(it.getCreateTime() == null ? "" : it.getCreateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))));
            sb.append('\n');
        }
        String stamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return fileDeliveryService.deliver(adminId, "REPORT_CSV", null,
                "启事数据报表 " + stamp,
                "item 全量 " + items.size() + " 行（含状态/审核/发布人）",
                sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), "csv", "text/csv");
    }

    /** CSV 单元格：含逗号/引号/换行时加引号转义；公式前缀字符额外加 ' 防注入（E-08B 延续）。 */
    private String csv(Object v) {
        String s = v == null ? "" : String.valueOf(v);
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r"))
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        if (!s.isEmpty() && ("=+-@".indexOf(s.charAt(0)) >= 0)) s = "'" + s;
        return s;
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

    // ---------------- E-02：审核/下架结果通知与理由 ----------------

    private static final int MAX_REASON = 200;   // E-02：理由上限，admin_log.detail(300) 与 message.content(300) 均可容纳

    /** E-02：给启事发布者写站内消息（type=audit，refId=itemId；与 ClaimService.push 模型一致）。 */
    private void push(Long userId, String title, String content, Long refId) {
        Message m = new Message();
        m.setUserId(userId);
        m.setType("audit");
        m.setTitle(title);
        m.setContent(content);
        m.setRefId(refId);
        m.setIsRead(0);
        messageMapper.insert(m);
    }

    /** E-02：理由校验与归一（null/空白 → null；超长 → 400）。 */
    private String checkedReason(String reason) {
        if (reason == null || reason.isBlank()) return null;
        String r = reason.trim();
        if (r.length() > MAX_REASON) throw new BusinessException(400, "驳回理由最多 " + MAX_REASON + " 字");
        return r;
    }

    private String logReason(String reason) {
        return reason != null ? "；理由=" + reason : "";
    }
}
