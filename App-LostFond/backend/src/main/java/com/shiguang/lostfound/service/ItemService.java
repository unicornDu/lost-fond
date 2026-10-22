package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.common.PageResult;
import com.shiguang.lostfound.context.UserContext;
import com.shiguang.lostfound.dto.ItemForm;
import com.shiguang.lostfound.entity.ClaimRecord;
import com.shiguang.lostfound.entity.Item;
import com.shiguang.lostfound.entity.ItemImage;
import com.shiguang.lostfound.entity.Message;
import com.shiguang.lostfound.entity.User;
import com.shiguang.lostfound.mapper.ClaimRecordMapper;
import com.shiguang.lostfound.mapper.ItemImageMapper;
import com.shiguang.lostfound.mapper.ItemMapper;
import com.shiguang.lostfound.mapper.MessageMapper;
import com.shiguang.lostfound.mapper.UserMapper;
import com.shiguang.lostfound.util.PHashUtil;
import com.shiguang.lostfound.vo.ItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * M2 发布 + M4 检索。
 * 审查修正：2.23 图片必填；2.17 VO 带 thumbUrl；2.8 VO 带 auditStatus；
 * 2.25 分页参数上限；2.32 浏览量原子自增；2.33 同访客 30 分钟去重；2.7 私信 sendDm。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final UserMapper userMapper;
    private final MessageMapper messageMapper;
    private final ClaimRecordMapper claimRecordMapper;
    private final FileStorageService fileStorage;
    private final PHashUtil pHashUtil;

    private static final DateTimeFormatter OUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    /** 修复 B-01：彻底不可见状态（详情/相似推荐对非本人非管理员返回 404）。 */
    private static final Set<String> HIDDEN = Set.of("已下架", "已关闭");
    /** 修复 B-02：墙页只展示"仍在流转"的启事——与 StatsService 的 CLOSED 口径统一，
     *  已找回/已认领不再上墙（首页统计与墙页 total 从此恒等）。 */
    private static final Set<String> WALL_HIDDEN = Set.of("已下架", "已关闭", "已找回", "已认领");
    private static final long MAX_PAGE_SIZE = 100;      // 审查 2.31⑤ 分页上限
    private static final long VIEW_DEDUP_MS = 30 * 60 * 1000L; // 审查 2.33 浏览去重窗口

    /** 浏览去重：key=itemId|clientKey → 上次计数时间（审查 2.33）。 */
    private final ConcurrentHashMap<String, Long> viewDedup = new ConcurrentHashMap<>();

    // ---------------- M2 发布 ----------------
    @Transactional
    public ItemVO publish(Long userId, ItemForm form, MultipartFile[] files) {
        // 审查 2.23：以图搜图是核心卖点，无图启事进不了索引 → 强制至少一张可读图片
        if (files == null || files.length == 0)
            throw new BusinessException("请至少上传一张物品图片（以图搜图依赖图片指纹）");
        // 修复 B-10：图片数量上限与 SRS FR2.1 对齐（1–3 张）；放在读字节之前，避免无效 IO
        if (files.length > 3)
            throw new BusinessException("最多上传 3 张图片");

        Item item = new Item();
        item.setType("found".equals(form.getType()) ? "found" : "lost");
        item.setTitle(form.getTitle().trim());
        item.setCategory(blankTo(form.getCategory(), "其他"));
        item.setLocation(blankTo(form.getLocation(), ""));
        // v2.41.1：坐标（GCJ-02）。只填一个或超出范围一律当"未标注"，不让半截数据进库
        if (validCoord(form.getLatitude(), form.getLongitude())) {
            item.setLatitude(round6(form.getLatitude()));
            item.setLongitude(round6(form.getLongitude()));
        }
        item.setHappenTime(parseTime(form.getHappenTime()));
        item.setDescription(blankTo(form.getDescription(), ""));
        item.setStatus("found".equals(item.getType()) ? "待认领" : "寻找中");
        item.setViews(0);
        item.setUserId(userId);
        item.setAuditStatus(0); // 审查 2.4：真审核流，待管理员通过
        itemMapper.insert(item);

        String cover = null;
        int stored = 0;
        int sort = 0;
        // 修复 B-11：磁盘写入不在数据库事务保护内——收集本事务已落盘的 URL，
        // 后续任何一步抛异常回滚时逐个补偿删除，避免孤儿文件
        List<String> storedUrls = new ArrayList<>();
        try {
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                byte[] bytes;
                try {
                    bytes = f.getBytes();
                } catch (Exception e) {
                    log.warn("读取上传图片失败，跳过", e);
                    continue;
                }
                String url = fileStorage.store(bytes, f.getOriginalFilename());
                storedUrls.add(url);
                if (cover == null) cover = url;
                Long phash = null;
                try {
                    phash = pHashUtil.hash(bytes);
                } catch (Exception e) {
                    log.warn("图片 pHash 计算失败，跳过该图索引: {}", url, e);
                }
                ItemImage img = new ItemImage();
                img.setItemId(item.getId());
                img.setImgUrl(url);
                img.setPhash(phash);
                img.setSort(sort++);
                itemImageMapper.insert(img);
                stored++;
            }
            // 语义说明（修复 B-11 明确）：坏图"跳过"、好图保留；仅当一张都没存下才整体失败
            if (stored == 0) throw new BusinessException("请至少上传一张有效的物品图片");

            item.setCoverImg(cover);
            itemMapper.updateById(item);
        } catch (RuntimeException e) {
            storedUrls.forEach(fileStorage::deleteQuietly);   // 补偿：回滚磁盘副作用
            throw e;
        }

        Map<Long, String> nick = loadNicknames(Set.of(userId));
        ItemVO vo = toVO(item, nick, null);
        vo.setImages(loadImageUrls(item.getId()));
        return vo;
    }

    // ---------------- M4 列表检索 ----------------
    public PageResult<ItemVO> list(String type, String category, String location,
                                   String keyword, String sort, long page, long size) {
        // 审查 2.25/2.31⑤：分页参数校验与上限
        page = Math.max(1, page);
        size = Math.min(MAX_PAGE_SIZE, Math.max(1, size));

        QueryWrapper<Item> qw = new QueryWrapper<>();
        qw.eq("audit_status", 1);
        qw.notIn("status", WALL_HIDDEN);   // 修复 B-02：与首页统计同口径，已团聚启事不上墙
        if (type != null && !type.isBlank()) qw.eq("type", type);
        if (category != null && !category.isBlank()) qw.eq("category", category);
        if (location != null && !location.isBlank()) qw.eq("location", location);
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            qw.and(w -> w.like("title", k).or().like("description", k));
        }
        if ("hot".equals(sort)) qw.orderByDesc("views");
        else qw.orderByDesc("create_time");

        Page<Item> p = itemMapper.selectPage(new Page<>(page, size), qw);
        Map<Long, String> nick = loadNicknames(p.getRecords().stream()
                .map(Item::getUserId).collect(Collectors.toSet()));
        List<ItemVO> records = p.getRecords().stream()
                .map(it -> toVO(it, nick, null)).collect(Collectors.toList());
        return PageResult.of(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    // ---------------- 详情（审查 2.32 原子自增 + 2.33 去重） ----------------
    @Transactional
    public ItemVO detail(Long id, Long currentUserId, String clientKey) {
        Item item = itemMapper.selectById(id);
        if (item == null) throw new BusinessException(404, "启事不存在或已被删除");

        // 修复 B-01：详情可见性判定——待审核/已下架/已关闭的启事，仅发布者本人与管理员可见；
        // 其他人一律 404（不用 403，避免用状态码泄露"该启事存在但未过审"）。
        boolean selfOrAdmin = (currentUserId != null && currentUserId.equals(item.getUserId()))
                || UserContext.isAdmin();
        if (!selfOrAdmin) {
            if (item.getAuditStatus() == null || item.getAuditStatus() != 1
                    || HIDDEN.contains(item.getStatus()))
                throw new BusinessException(404, "启事不存在或已被删除");
        }

        String dedupKey = id + "|" + (clientKey == null ? "_" : clientKey);
        long now = System.currentTimeMillis();
        // 修复 B-07：去重表只增不减会慢性吃内存——超阈值时惰性清理过期条目（不引新依赖的简单方案）
        if (viewDedup.size() > 10000)
            viewDedup.entrySet().removeIf(e -> now - e.getValue() > VIEW_DEDUP_MS);
        Long last = viewDedup.get(dedupKey);
        if (last == null || now - last > VIEW_DEDUP_MS) {
            viewDedup.put(dedupKey, now);
            itemMapper.incrementViews(id);   // UPDATE item SET views = views + 1
            item.setViews((item.getViews() == null ? 0 : item.getViews()) + 1);
        }

        List<ItemImage> imgs = itemImageMapper.selectList(
                new QueryWrapper<ItemImage>().eq("item_id", id).orderByAsc("sort"));
        Map<Long, String> nick = loadNicknames(Set.of(item.getUserId()));
        ItemVO vo = toVO(item, nick, currentUserId);
        vo.setImages(imgs.stream().map(ItemImage::getImgUrl).collect(Collectors.toList()));
        // 2026-10-18 审查修复：本人认领状态随详情一次返回，前端不再额外请求 /claims/my（省一次往返）
        if (currentUserId != null && !currentUserId.equals(item.getUserId())) {
            ClaimRecord mine = claimRecordMapper.selectOne(new QueryWrapper<ClaimRecord>()
                    .eq("item_id", id).eq("claimant_id", currentUserId)
                    .orderByDesc("create_time").last("LIMIT 1"));
            if (mine != null) {
                vo.setMyClaimStatus(mine.getStatus());
                vo.setMyClaimReply(mine.getReply());
            }
        }
        return vo;
    }

    /**
     * 失主标记完成（修复 Z-02：补状态白名单守卫 + 批量关闭悬挂在审申请 + 事务）。
     * 白名单与 ClaimService.apply 一致；待审核启事（audit=0、status 恰在白名单内）本人可标记——
     * 其对他人不可见（B-01），标记无外部影响，属有意放行，勿加 auditStatus 判断。
     */
    @Transactional
    public void markFound(Long id, Long userId) {
        Item item = itemMapper.selectById(id);
        if (item == null) throw new BusinessException(404, "启事不存在");
        if (!item.getUserId().equals(userId)) throw new BusinessException(403, "只能操作自己发布的启事");
        if (!"寻找中".equals(item.getStatus()) && !"待认领".equals(item.getStatus())
                && !"认领申请中".equals(item.getStatus()))
            throw new BusinessException(400, "该启事当前状态不允许标记完成");
        String done = "lost".equals(item.getType()) ? "已找回" : "已认领";
        item.setStatus(done);
        itemMapper.updateById(item);
        // "认领申请中"下仍有在审申请：直接写终态会让它们悬挂（ClaimService.audit 的已完成守卫拒绝再处理），
        // 批量关闭并逐个通知，与 ClaimService.audit 通过分支的自动关闭语义对齐（refId=itemId 口径）
        List<ClaimRecord> pending = claimRecordMapper.selectList(new QueryWrapper<ClaimRecord>()
                .eq("item_id", id).eq("status", "审核中"));
        for (ClaimRecord c : pending) {
            String reply = "失主已将物品标记为「" + done + "」，申请自动关闭";
            c.setStatus("已拒绝");
            c.setReply(reply);
            claimRecordMapper.updateById(c);
            Message m = new Message();
            m.setUserId(c.getClaimantId());
            m.setType("claim");
            m.setTitle("认领申请未通过");
            m.setContent("你对「" + item.getTitle() + "」的申请未通过：" + reply);
            m.setRefId(id);
            m.setIsRead(0);
            messageMapper.insert(m);
        }
    }

    /** 审查 2.7：私信接真——给启事发布者发一条站内消息。 */
    @Transactional
    public void sendDm(Long itemId, Long fromUserId, String content) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) throw new BusinessException(404, "启事不存在");
        if (item.getUserId().equals(fromUserId)) throw new BusinessException("不能给自己私信");
        if (content == null || content.isBlank()) throw new BusinessException("私信内容不能为空");
        User from = userMapper.selectById(fromUserId);
        String full = (from == null ? "同学" : from.getNickname()) + " 就「" + item.getTitle() + "」给你留言：" + content.trim();
        // 修复 B-10：message.content 列宽 VARCHAR(300)，拼前缀后的总长也要兜底校验，避免撞库约束
        if (full.length() > 300) throw new BusinessException(400, "私信太长（含引用标题后最多 300 字），请精简后重试");
        Message m = new Message();
        m.setUserId(item.getUserId());
        m.setFromUserId(fromUserId);   // E-03：带发送者，收信人可回复
        m.setType("dm");
        m.setTitle("收到新私信");
        m.setContent(full);
        m.setRefId(itemId);
        m.setIsRead(0);
        messageMapper.insert(m);
    }

    public List<ItemVO> myPublishes(Long userId) {
        List<Item> items = itemMapper.selectList(new QueryWrapper<Item>()
                .eq("user_id", userId).orderByDesc("create_time"));
        if (items.isEmpty()) return List.of();
        Map<Long, String> nick = loadNicknames(Set.of(userId));
        // 2026-10-18 审查修复：原实现每条启事单独查一次图片（N+1）；改为一次 IN 查询后分组
        List<Long> ids = items.stream().map(Item::getId).collect(Collectors.toList());
        Map<Long, List<String>> imgMap = itemImageMapper.selectList(
                        new QueryWrapper<ItemImage>().in("item_id", ids).orderByAsc("sort"))
                .stream()
                .collect(Collectors.groupingBy(ItemImage::getItemId,
                        Collectors.mapping(ItemImage::getImgUrl, Collectors.toList())));
        return items.stream().map(it -> {
            ItemVO vo = toVO(it, nick, userId);
            vo.setImages(imgMap.getOrDefault(it.getId(), List.of()));
            return vo;
        }).collect(Collectors.toList());
    }

    // ---------------- E-04：他人主页（公开口径） ----------------

    /**
     * 公开可见口径（E-04）：audit_status=1 且 status ∉ HIDDEN（已下架/已关闭不可见），
     * 已找回/已认领的团聚历史保留可见——与 detail() 的可见性判定同源（复用 HIDDEN），
     * 不与墙页 WALL_HIDDEN 混用（墙页口径是"仍在流转"，主页是"该用户的公开启事"）。
     */
    private QueryWrapper<Item> publicItemWrapper(Long userId) {
        QueryWrapper<Item> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        qw.eq("audit_status", 1);
        qw.notIn("status", HIDDEN);
        return qw;
    }

    public long publicItemCount(Long userId) {
        return itemMapper.selectCount(publicItemWrapper(userId));
    }

    /** 他人主页的公开启事分页（E-04）。 */
    public PageResult<ItemVO> listPublicByUser(Long userId, long page, long size) {
        page = Math.max(1, page);
        size = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        QueryWrapper<Item> qw = publicItemWrapper(userId);
        qw.orderByDesc("create_time");
        Page<Item> p = itemMapper.selectPage(new Page<>(page, size), qw);
        Map<Long, String> nick = loadNicknames(p.getRecords().stream()
                .map(Item::getUserId).collect(Collectors.toSet()));
        List<ItemVO> records = p.getRecords().stream()
                .map(it -> toVO(it, nick, null)).collect(Collectors.toList());
        return PageResult.of(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    // ---------------- 组装工具 ----------------
    public ItemVO toVO(Item it, Map<Long, String> nick, Long currentUserId) {
        ItemVO vo = new ItemVO();
        vo.setId(it.getId());
        vo.setType(it.getType());
        vo.setTitle(it.getTitle());
        vo.setCategory(it.getCategory());
        vo.setLocation(it.getLocation());
        vo.setLatitude(it.getLatitude());
        vo.setLongitude(it.getLongitude());
        vo.setTime(it.getHappenTime() != null ? it.getHappenTime().format(OUT)
                : (it.getCreateTime() != null ? it.getCreateTime().format(OUT) : ""));
        vo.setStatus(it.getStatus());
        vo.setOwner(nick.getOrDefault(it.getUserId(), "同学"));
        vo.setViews(it.getViews() == null ? 0 : it.getViews());
        vo.setDesc(it.getDescription());
        vo.setImg(it.getCoverImg());
        vo.setThumb(fileStorage.thumbUrl(it.getCoverImg()));   // 审查 2.17
        vo.setAuditStatus(it.getAuditStatus());                // 审查 2.8
        vo.setUserId(it.getUserId());
        vo.setMine(currentUserId != null && currentUserId.equals(it.getUserId()));
        return vo;
    }

    public Map<Long, String> loadNicknames(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        Set<Long> ids = userIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        List<User> users = userMapper.selectBatchIds(ids);
        return users.stream().collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
    }

    private List<String> loadImageUrls(Long itemId) {
        return itemImageMapper.selectList(new QueryWrapper<ItemImage>()
                        .eq("item_id", itemId).orderByAsc("sort"))
                .stream().map(ItemImage::getImgUrl).collect(Collectors.toList());
    }

    /** 审查 2.28：时间解析失败抛业务异常，不再静默改当前时间。 */
    private LocalDateTime parseTime(String s) {
        if (s == null || s.isBlank()) throw new BusinessException("请选择丢失/捡到时间");
        String v = s.trim().replace("T", " ");
        for (String pat : List.of("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm")) {
            try {
                return LocalDateTime.parse(v, DateTimeFormatter.ofPattern(pat));
            } catch (Exception ignored) { }
        }
        try {
            return LocalDateTime.parse(v, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            throw new BusinessException("时间格式不正确");
        }
    }

    private String blankTo(String s, String def) {
        return (s == null || s.isBlank()) ? def : s.trim();
    }

    /** v2.41.1：经纬度必须成对出现且在合法范围内（GCJ-02）。 */
    private boolean validCoord(Double lat, Double lng) {
        return lat != null && lng != null
                && !lat.isNaN() && !lng.isNaN()
                && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    /** 小数 6 位（约 0.1 m），与 DECIMAL(10,6) 对齐，避免插入时被库四舍五入产生误差。 */
    private double round6(Double v) {
        return Math.round(v * 1_000_000d) / 1_000_000d;
    }
}
