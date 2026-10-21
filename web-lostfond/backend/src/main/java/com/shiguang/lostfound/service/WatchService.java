package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.common.TxUtil;
import com.shiguang.lostfound.entity.Item;
import com.shiguang.lostfound.entity.ItemImage;
import com.shiguang.lostfound.entity.Message;
import com.shiguang.lostfound.entity.WatchSubscription;
import com.shiguang.lostfound.entity.WatchTarget;
import com.shiguang.lostfound.mapper.ItemImageMapper;
import com.shiguang.lostfound.mapper.ItemMapper;
import com.shiguang.lostfound.mapper.MessageMapper;
import com.shiguang.lostfound.mapper.WatchSubscriptionMapper;
import com.shiguang.lostfound.mapper.WatchTargetMapper;
import com.shiguang.lostfound.util.PHashUtil;
import com.shiguang.lostfound.vo.WatchOverviewVO;
import com.shiguang.lostfound.vo.WatchPlanVO;
import com.shiguang.lostfound.vo.WatchSubscriptionVO;
import com.shiguang.lostfound.vo.WatchTargetVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * v2.20 守望订阅（把「这次没搜到也没关系」做成真的）。
 *
 * <p>链路：以图搜图页打开守望开关 → 服务端用同一条 pHash 口径存下照片指纹（watch_target）
 * → 管理员审核通过一条<b>招领</b>启事时，反查所有生效守望位，汉明距离 ≤ 方案阈值即写站内通知
 * （type=watch，refId=itemId）→ 侧栏通知角标 + 通知页「查看启事」闭环。</p>
 *
 * <p>方案权益（守望位 / 阈值 / 保留天数）只在这里定义，前端通过 GET /api/watch/plans 读取数值、
 * 按 code 走 i18n 渲染文案，避免两套数字各写一遍。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatchService {

    public static final String PLAN_LIGHT = "light";
    public static final String PLAN_STEADY = "steady";
    public static final String PLAN_CROWD = "crowd";
    public static final String CYCLE_MONTH = "month";
    public static final String CYCLE_SEMESTER = "semester";

    private static final int MONTH_DAYS = 30;
    private static final int SEMESTER_DAYS = 120;
    private static final int MAX_LABEL = 60;
    private static final DateTimeFormatter OUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 方案定义（价格单位：元；课程演示不产生真实扣费）。 */
    public record Plan(String code, String name, int price, int termPrice,
                       int quota, int threshold, int retentionDays, boolean popular) {
    }

    private static final List<Plan> PLANS = List.of(
            new Plan(PLAN_LIGHT, "轻守", 0, 0, 1, 10, 7, false),
            new Plan(PLAN_STEADY, "长守", 6, 20, 5, 12, 30, true),
            new Plan(PLAN_CROWD, "众守", 18, 60, 20, 14, 90, false)
    );
    private static final Map<String, Plan> BY_CODE = PLANS.stream()
            .collect(Collectors.toMap(Plan::code, p -> p));

    private final WatchSubscriptionMapper subscriptionMapper;
    private final WatchTargetMapper targetMapper;
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final MessageMapper messageMapper;
    private final FileStorageService fileStorage;
    private final PHashUtil pHashUtil;

    // ---------------- 方案目录 ----------------

    public List<WatchPlanVO> plans() {
        return PLANS.stream().map(this::toPlanVO).collect(Collectors.toList());
    }

    public Plan planOf(String code) {
        Plan p = code == null ? null : BY_CODE.get(code);
        if (p == null) throw new BusinessException("未知的守望方案");
        return p;
    }

    // ---------------- 总览 ----------------

    /** 读总览：顺带补建免费订阅、清理过期守望位（低成本的惰性维护，不引定时任务）。 */
    @Transactional
    public WatchOverviewVO overview(Long userId) {
        WatchSubscription s = ensureSubscription(userId);
        expireStaleTargets(userId);
        List<WatchTarget> targets = targetMapper.selectList(new QueryWrapper<WatchTarget>()
                .eq("user_id", userId).eq("status", 1).orderByDesc("create_time"));
        Plan p = planOf(s.getPlanCode());

        WatchOverviewVO vo = new WatchOverviewVO();
        vo.setPlan(toSubscriptionVO(s, p));
        vo.setQuota(p.quota());
        vo.setUsed(targets.size());
        vo.setTargets(toTargetVOs(targets));
        return vo;
    }

    // ---------------- 开通 / 切换方案 ----------------

    @Transactional
    public WatchSubscriptionVO subscribe(Long userId, String planCode, String cycle) {
        Plan p = planOf(planCode);
        String c = CYCLE_SEMESTER.equals(cycle) ? CYCLE_SEMESTER : CYCLE_MONTH;
        if (PLAN_LIGHT.equals(p.code())) c = CYCLE_MONTH;   // 免费档没有周期概念

        WatchSubscription s = ensureSubscription(userId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expire = PLAN_LIGHT.equals(p.code())
                ? null : now.plusDays(CYCLE_SEMESTER.equals(c) ? SEMESTER_DAYS : MONTH_DAYS);
        persistPlan(s, p, c, now, expire);

        // 降档时不能让旧的高配额永远生效：按新配额保留最近的若干守望位
        int dropped = enforceQuota(userId, p.quota());
        if (dropped > 0) log.info("守望方案切换 {}，用户 {} 结束 {} 个超额守望位", p.code(), userId, dropped);
        return toSubscriptionVO(s, p);
    }

    // ---------------- 守望位 ----------------

    @Transactional
    public WatchTargetVO addTarget(Long userId, MultipartFile file, String label, Long sourceItemId) {
        if (file == null || file.isEmpty()) throw new BusinessException("请上传要守望的照片");
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException("图片读取失败");
        }
        fileStorage.validateImage(file.getOriginalFilename(), bytes);
        long hash;
        try {
            hash = pHashUtil.hash(bytes);
        } catch (Exception e) {
            throw new BusinessException("图片解析失败，请上传 JPG/PNG");
        }

        WatchSubscription s = ensureSubscription(userId);
        Plan p = planOf(s.getPlanCode());
        expireStaleTargets(userId);
        long used = targetMapper.selectCount(new QueryWrapper<WatchTarget>()
                .eq("user_id", userId).eq("status", 1));
        if (used >= p.quota()) {
            throw new BusinessException("守望位已满（当前方案 " + p.quota() + " 个），可在「守望订阅」升级方案");
        }

        String url = fileStorage.store(bytes, file.getOriginalFilename());
        LocalDateTime expire = LocalDateTime.now().plusDays(p.retentionDays());
        if (s.getExpireTime() != null && s.getExpireTime().isBefore(expire)) expire = s.getExpireTime();

        WatchTarget t = new WatchTarget();
        t.setUserId(userId);
        t.setPhash(hash);
        t.setLabel(normalizeLabel(label));
        t.setImgUrl(url);
        t.setSourceItemId(sourceItemId);
        t.setStatus(1);
        t.setHitCount(0);
        t.setExpireTime(expire);
        try {
            targetMapper.insert(t);
        } catch (RuntimeException e) {
            // 与 ItemService 同一取舍：数据库没写成，磁盘文件不能留
            fileStorage.deleteQuietly(url);
            throw e;
        }
        return toTargetVO(t, Map.of());
    }

    /** 结束守望：只置 status=0，保留行以便「命中记录/历史」；缩略图延迟到事务提交后删。 */
    @Transactional
    public void removeTarget(Long userId, Long id) {
        WatchTarget t = targetMapper.selectById(id);
        if (t == null || !Objects.equals(t.getUserId(), userId)) {
            throw new BusinessException(404, "守望记录不存在");
        }
        if (t.getStatus() != null && t.getStatus() == 0) return;
        t.setStatus(0);
        targetMapper.updateById(t);
        String url = t.getImgUrl();
        if (url != null && !url.isBlank()) TxUtil.afterCommit(() -> fileStorage.deleteQuietly(url));
    }

    // ---------------- 审核通过时的反查推送 ----------------

    /**
     * 新招领通过审核 → 与所有生效守望位做汉明距离比对，命中即通知守望人。
     * 只比 found（招领）：守望的语义是「等别人捡到我的东西」，与以图搜图页文案一致。
     *
     * @return 命中并推送的守望位数量（供日志/测试断言）
     */
    @Transactional
    public int matchOnApprove(Item item) {
        if (item == null || item.getId() == null || !"found".equals(item.getType())) return 0;
        List<Long> hashes = itemImageMapper.selectList(new QueryWrapper<ItemImage>()
                        .eq("item_id", item.getId()).isNotNull("phash")).stream()
                .map(ItemImage::getPhash).filter(Objects::nonNull).collect(Collectors.toList());
        if (hashes.isEmpty()) return 0;

        LocalDateTime now = LocalDateTime.now();
        List<WatchTarget> targets = targetMapper.selectList(new QueryWrapper<WatchTarget>()
                .eq("status", 1)
                .ne("user_id", item.getUserId())
                .and(w -> w.isNull("expire_time").or().gt("expire_time", now)));
        if (targets.isEmpty()) return 0;

        Map<Long, Integer> thresholdCache = new HashMap<>();
        int hits = 0;
        for (WatchTarget t : targets) {
            if (t.getPhash() == null) continue;
            int dist = 64;
            for (Long h : hashes) dist = Math.min(dist, Long.bitCount(t.getPhash() ^ h));
            int threshold = thresholdCache.computeIfAbsent(t.getUserId(), this::thresholdOf);
            if (dist > threshold) continue;

            int similarity = (int) Math.round((1 - dist / 64.0) * 100);
            pushWatchHit(t, item, similarity);
            t.setHitCount((t.getHitCount() == null ? 0 : t.getHitCount()) + 1);
            t.setLastHitItemId(item.getId());
            targetMapper.updateById(t);
            hits++;
        }
        if (hits > 0) log.info("守望命中：启事 {} 命中 {} 个守望位", item.getId(), hits);
        return hits;
    }

    /** 取用户当前方案阈值；没订阅过按免费档算（不在这里补建订阅，避免匹配过程产生写放大）。 */
    private int thresholdOf(Long userId) {
        WatchSubscription s = subscriptionMapper.selectOne(new QueryWrapper<WatchSubscription>()
                .eq("user_id", userId).last("LIMIT 1"));
        if (s == null || s.getPlanCode() == null) return planOf(PLAN_LIGHT).threshold();
        Plan p = BY_CODE.get(s.getPlanCode());
        return p == null ? planOf(PLAN_LIGHT).threshold() : p.threshold();
    }

    private void pushWatchHit(WatchTarget t, Item item, int similarity) {
        Message m = new Message();
        m.setUserId(t.getUserId());
        m.setType("watch");
        m.setTitle("守望命中 · 相似度 " + similarity + "%");
        m.setContent("你守望的「" + (t.getLabel() == null || t.getLabel().isBlank() ? "照片" : t.getLabel())
                + "」与刚通过审核的招领《" + item.getTitle() + "》相似度 " + similarity + "%，点开看看是不是同一件。");
        m.setRefId(item.getId());
        m.setIsRead(0);
        messageMapper.insert(m);
    }

    // ---------------- 内部 ----------------

    /** 没有订阅记录 → 补一条轻守；付费档到期 → 回落轻守并清掉超额守望位。 */
    private WatchSubscription ensureSubscription(Long userId) {
        WatchSubscription s = subscriptionMapper.selectOne(new QueryWrapper<WatchSubscription>()
                .eq("user_id", userId).last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (s == null) {
            s = new WatchSubscription();
            s.setUserId(userId);
            s.setPlanCode(PLAN_LIGHT);
            s.setCycle(CYCLE_MONTH);
            s.setStatus(1);
            s.setStartTime(now);
            s.setExpireTime(null);
            subscriptionMapper.insert(s);
            return s;
        }
        if (s.getExpireTime() != null && !s.getExpireTime().isAfter(now)) {
            Plan light = planOf(PLAN_LIGHT);
            persistPlan(s, light, CYCLE_MONTH, now, null);
            enforceQuota(userId, light.quota());
        }
        return s;
    }

    /**
     * 落库订阅状态。
     * 必须用 UpdateWrapper 显式 set：MyBatis-Plus 的 updateById 默认跳过 null 字段，
     * 降档到轻守时 expire_time 会残留上一次付费档的日期（表现为「轻守还显示到期时间」）。
     */
    private void persistPlan(WatchSubscription s, Plan p, String cycle,
                             LocalDateTime start, LocalDateTime expire) {
        s.setPlanCode(p.code());
        s.setCycle(cycle);
        s.setStatus(1);
        s.setStartTime(start);
        s.setExpireTime(expire);
        subscriptionMapper.update(null, new UpdateWrapper<WatchSubscription>()
                .eq("id", s.getId())
                .set("plan_code", p.code())
                .set("cycle", cycle)
                .set("status", 1)
                .set("start_time", start)
                .set("expire_time", expire));
    }

    private void expireStaleTargets(Long userId) {
        WatchTarget patch = new WatchTarget();
        patch.setStatus(0);
        targetMapper.update(patch, new QueryWrapper<WatchTarget>()
                .eq("user_id", userId).eq("status", 1)
                .isNotNull("expire_time").lt("expire_time", LocalDateTime.now()));
    }

    /** 按配额保留最近的 N 个守望位，其余置为已结束。返回被结束的数量。 */
    private int enforceQuota(Long userId, int quota) {
        List<WatchTarget> active = targetMapper.selectList(new QueryWrapper<WatchTarget>()
                .eq("user_id", userId).eq("status", 1).orderByDesc("create_time"));
        if (active.size() <= quota) return 0;
        int dropped = 0;
        for (int i = Math.max(0, quota); i < active.size(); i++) {
            WatchTarget t = active.get(i);
            t.setStatus(0);
            targetMapper.updateById(t);
            dropped++;
        }
        return dropped;
    }

    private String normalizeLabel(String label) {
        if (label == null || label.isBlank()) return "守望照片";
        String s = label.trim();
        return s.length() > MAX_LABEL ? s.substring(0, MAX_LABEL) : s;
    }

    private WatchPlanVO toPlanVO(Plan p) {
        WatchPlanVO v = new WatchPlanVO();
        v.setCode(p.code());
        v.setName(p.name());
        v.setPrice(p.price());
        v.setTermPrice(p.termPrice());
        v.setQuota(p.quota());
        v.setThreshold(p.threshold());
        v.setRetentionDays(p.retentionDays());
        v.setPopular(p.popular());
        return v;
    }

    private WatchSubscriptionVO toSubscriptionVO(WatchSubscription s, Plan p) {
        WatchSubscriptionVO v = new WatchSubscriptionVO();
        v.setPlanCode(p.code());
        v.setPlanName(p.name());
        v.setCycle(s.getCycle());
        v.setStatus(s.getStatus());
        v.setStartTime(s.getStartTime() == null ? null : s.getStartTime().format(OUT));
        v.setExpireTime(s.getExpireTime() == null ? null : s.getExpireTime().format(OUT));
        v.setQuota(p.quota());
        v.setThreshold(p.threshold());
        v.setRetentionDays(p.retentionDays());
        v.setPrice(p.price());
        v.setTermPrice(p.termPrice());
        return v;
    }

    private List<WatchTargetVO> toTargetVOs(List<WatchTarget> targets) {
        Set<Long> itemIds = targets.stream().map(WatchTarget::getLastHitItemId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Item> items = itemIds.isEmpty() ? Map.of()
                : itemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, i -> i, (a, b) -> a));
        List<WatchTargetVO> out = new ArrayList<>(targets.size());
        for (WatchTarget t : targets) out.add(toTargetVO(t, items));
        return out;
    }

    private WatchTargetVO toTargetVO(WatchTarget t, Map<Long, Item> items) {
        WatchTargetVO v = new WatchTargetVO();
        v.setId(t.getId());
        v.setLabel(t.getLabel());
        v.setImgUrl(t.getImgUrl());
        v.setHitCount(t.getHitCount() == null ? 0 : t.getHitCount());
        v.setLastHitItemId(t.getLastHitItemId());
        Item hit = t.getLastHitItemId() == null ? null : items.get(t.getLastHitItemId());
        if (hit != null) v.setLastHitTitle(hit.getTitle());
        v.setCreateTime(t.getCreateTime() == null ? null : t.getCreateTime().format(OUT));
        if (t.getExpireTime() == null) {
            v.setRemainingDays(null);
        } else {
            long minutes = Duration.between(LocalDateTime.now(), t.getExpireTime()).toMinutes();
            v.setRemainingDays((int) Math.max(0, Math.ceil(minutes / 1440.0)));
            v.setExpireTime(t.getExpireTime().format(OUT));
        }
        return v;
    }
}
