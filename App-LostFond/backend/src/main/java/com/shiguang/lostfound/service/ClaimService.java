package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.common.TxUtil;
import com.shiguang.lostfound.dto.ClaimAuditReq;
import com.shiguang.lostfound.dto.ClaimReq;
import com.shiguang.lostfound.entity.ClaimRecord;
import com.shiguang.lostfound.entity.Item;
import com.shiguang.lostfound.entity.Message;
import com.shiguang.lostfound.entity.User;
import com.shiguang.lostfound.mapper.ClaimRecordMapper;
import com.shiguang.lostfound.mapper.ItemMapper;
import com.shiguang.lostfound.mapper.MessageMapper;
import com.shiguang.lostfound.mapper.UserMapper;
import com.shiguang.lostfound.vo.ClaimVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * M5 认领与消息模块。
 * 审查 2.6 修正后的状态机：
 *  - apply() 白名单守卫：仅「寻找中/待认领/认领申请中」可申请（已下架/已关闭/已完成不可）。
 *  - audit() 通过：物品置 已找回/已认领；同物品其余「审核中」申请批量置「已拒绝」并逐个通知。
 *  - audit() 拒绝：仅当同物品已无其他「审核中」申请时才回退物品状态（避免状态自相矛盾）。
 *  - audit() 守卫：物品已完成认领 / 申请已处理过 / 非发布者 → 拒绝。
 * 审查 2.5：ClaimVO 回填申请人昵称/学号/凭证/联系方式，杜绝盲审。
 *
 * 2026-10-18 审查修复：
 *  - 并发原子性：通过/拒绝改为「条件更新」（WHERE status='审核中' / item 状态白名单），
 *    受影响行数为 0 即视为并发冲突抛业务异常，杜绝并发双审产生两条「已通过」。
 *  - 认领证明 PDF 改为事务提交后生成（TxUtil.afterCommit）：原实现虽 try/catch，
 *    但内层 @Transactional 抛异常会把外层事务标记 rollback-only，导致「PDF 失败不阻断审核」失效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRecordMapper claimMapper;
    private final ItemMapper itemMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final ItemService itemService;
    private final PdfService pdfService;
    private final FileDeliveryService fileDeliveryService;

    private static final DateTimeFormatter OUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Transactional
    public void apply(Long itemId, Long claimantId, ClaimReq req) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) throw new BusinessException(404, "启事不存在");
        if (item.getUserId().equals(claimantId)) throw new BusinessException("不能认领自己发布的启事");
        // 审查 2.6 白名单守卫 + 05 修正：「认领申请中」必须放行——竞争状态机（附录 D4：通过一条自动关闭
        // 其余在审申请）依赖同一物品存在多条"审核中"；原白名单漏了它，第二个申请人永远进不来（TC-竞争-01 无法成立）
        if (!"寻找中".equals(item.getStatus()) && !"待认领".equals(item.getStatus())
                && !"认领申请中".equals(item.getStatus()))
            throw new BusinessException("该启事当前状态不可申请认领");

        Long dup = claimMapper.selectCount(new QueryWrapper<ClaimRecord>()
                .eq("item_id", itemId).eq("claimant_id", claimantId).eq("status", "审核中"));
        if (dup != null && dup > 0) throw new BusinessException("你已提交过认领申请，请等待发布者审核");

        ClaimRecord c = new ClaimRecord();
        c.setItemId(itemId);
        c.setClaimantId(claimantId);
        c.setProof(req.getProof());
        c.setContact(req.getContact());
        c.setStatus("审核中");
        claimMapper.insert(c);

        item.setStatus("认领申请中");
        itemMapper.updateById(item);

        push(item.getUserId(), "claim", "收到新的认领申请",
                "有人申请认领「" + item.getTitle() + "」，快去个人中心查看凭证并审核。", itemId);
    }

    /** 审查 2.6 / 附录 D4：完整审核逻辑（2026-10-18 并发原子化）。 */
    @Transactional
    public void audit(Long claimId, Long publisherId, ClaimAuditReq req) {
        ClaimRecord c = claimMapper.selectById(claimId);
        if (c == null) throw new BusinessException(404, "认领申请不存在");
        Item item = itemMapper.selectById(c.getItemId());
        if (item == null) throw new BusinessException(404, "启事不存在");
        if (!item.getUserId().equals(publisherId)) throw new BusinessException(403, "只有发布者本人可以审核");
        if (!"审核中".equals(c.getStatus())) throw new BusinessException("该申请已处理过");
        // 守卫：物品已完成认领则不再受理
        if ("已认领".equals(item.getStatus()) || "已找回".equals(item.getStatus()))
            throw new BusinessException("该物品已完成认领，申请已关闭");

        boolean lost = "lost".equals(item.getType());

        if (req.isApprove()) {
            // ① 物品条件更新：把状态 CAS 到终态。并发双审时只有一个事务能命中状态白名单，
            //    另一个受影响行数为 0 → 直接失败并回滚，避免"两条已通过 / 两份证明"。
            int itemRows = itemMapper.update(null, new UpdateWrapper<Item>()
                    .eq("id", item.getId())
                    .in("status", "寻找中", "待认领", "认领申请中")
                    .set("status", lost ? "已找回" : "已认领"));
            if (itemRows == 0)
                throw new BusinessException("该物品已完成认领或状态已变更，申请已关闭");

            // ② 认领记录条件更新：已处理过的申请受影响行数为 0
            String reply = blankTo(req.getReply(), "已通过认领，请联系交接");
            int claimRows = claimMapper.update(null, new UpdateWrapper<ClaimRecord>()
                    .eq("id", c.getId()).eq("status", "审核中")
                    .set("status", "已通过").set("reply", reply));
            if (claimRows == 0) throw new BusinessException("该申请已处理过");
            c.setStatus("已通过");
            c.setReply(reply);

            // ③ 关闭同物品其余「审核中」申请并逐个通知（同一事务内，先查后改；主记录已 CAS 成功）
            List<ClaimRecord> others = claimMapper.selectList(new QueryWrapper<ClaimRecord>()
                    .eq("item_id", c.getItemId()).eq("status", "审核中").ne("id", c.getId()));
            for (ClaimRecord o : others) {
                o.setStatus("已拒绝");
                o.setReply("物品已被其他申请人认领");
                claimMapper.updateById(o);
                push(o.getClaimantId(), "claim", "认领申请未通过",
                        "你对「" + item.getTitle() + "」的申请未通过：物品已被其他申请人认领。", item.getId());
            }
            push(c.getClaimantId(), "claim", "认领申请已通过",
                    "你对「" + item.getTitle() + "」的认领申请已通过：" + c.getReply(), item.getId());

            // ④ 认领证明 PDF：事务提交后再生成并交付（失败仅记日志，绝不影响审核结果）
            Long itemId = item.getId();
            Long id = c.getId();
            TxUtil.afterCommit(() -> issueCertificates(itemId, id));
        } else {
            String reply = blankTo(req.getReply(), "特征不符，已拒绝");
            int claimRows = claimMapper.update(null, new UpdateWrapper<ClaimRecord>()
                    .eq("id", c.getId()).eq("status", "审核中")
                    .set("status", "已拒绝").set("reply", reply));
            if (claimRows == 0) throw new BusinessException("该申请已处理过");
            c.setStatus("已拒绝");
            c.setReply(reply);

            // 仅当已无其他「审核中」申请时才回退物品状态；同样走条件更新，
            // 避免把并发中已被置为终态的物品改回「寻找中」
            Long pending = claimMapper.selectCount(new QueryWrapper<ClaimRecord>()
                    .eq("item_id", c.getItemId()).eq("status", "审核中"));
            if (pending == null || pending == 0) {
                itemMapper.update(null, new UpdateWrapper<Item>()
                        .eq("id", item.getId()).eq("status", "认领申请中")
                        .set("status", lost ? "寻找中" : "待认领"));
            }
            push(c.getClaimantId(), "claim", "认领申请未通过",
                    "你对「" + item.getTitle() + "」的申请未通过：" + c.getReply(), item.getId());
        }
    }

    /**
     * 事务提交后生成认领证明并交付双方（P1）。
     * 独立重查数据，避免依赖事务内可能已变化的实体；任何失败只记日志。
     */
    private void issueCertificates(Long itemId, Long claimId) {
        try {
            Item item = itemMapper.selectById(itemId);
            ClaimRecord c = claimMapper.selectById(claimId);
            if (item == null || c == null) return;
            User owner = userMapper.selectById(item.getUserId());
            User claimant = userMapper.selectById(c.getClaimantId());
            byte[] pdf = pdfService.claimCertificate(item, c, owner, claimant);
            String certTitle = "认领证明 #" + c.getId();
            String desc = "物品「" + item.getTitle() + "」认领成功交接凭证";
            fileDeliveryService.deliver(item.getUserId(), "CLAIM_CERT", item.getId(),
                    certTitle, desc, pdf, "pdf", "application/pdf");
            fileDeliveryService.deliver(c.getClaimantId(), "CLAIM_CERT", item.getId(),
                    certTitle, desc, pdf, "pdf", "application/pdf");
        } catch (Exception e) {
            log.warn("认领证明生成/交付失败（不阻断审核）: itemId={} claimId={}", itemId, claimId, e);
        }
    }

    /** 我发起的认领（个人中心「我的认领」）。 */
    public List<ClaimVO> myClaims(Long userId) {
        List<ClaimRecord> list = claimMapper.selectList(new QueryWrapper<ClaimRecord>()
                .eq("claimant_id", userId).orderByDesc("create_time"));
        return build(list);
    }

    /** 我发布的启事收到的认领申请（发布者审核用，含凭证/申请人/联系方式）。 */
    public List<ClaimVO> receivedClaims(Long publisherId) {
        List<Item> myItems = itemMapper.selectList(new QueryWrapper<Item>().eq("user_id", publisherId));
        if (myItems.isEmpty()) return List.of();
        List<Long> itemIds = myItems.stream().map(Item::getId).collect(Collectors.toList());
        List<ClaimRecord> list = claimMapper.selectList(new QueryWrapper<ClaimRecord>()
                .in("item_id", itemIds).orderByDesc("create_time"));
        return build(list);
    }

    /** 审查 2.5 / 附录 D2：回填申请人信息 + 凭证 + 联系方式。 */
    private List<ClaimVO> build(List<ClaimRecord> list) {
        if (list.isEmpty()) return List.of();
        Set<Long> itemIds = list.stream().map(ClaimRecord::getItemId).collect(Collectors.toSet());
        Map<Long, Item> itemMap = itemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, i -> i, (a, b) -> a));
        Set<Long> claimantIds = list.stream().map(ClaimRecord::getClaimantId).collect(Collectors.toSet());
        Map<Long, User> userMap = claimantIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(claimantIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        List<ClaimVO> result = new ArrayList<>();
        for (ClaimRecord c : list) {
            Item it = itemMap.get(c.getItemId());
            if (it == null) continue;
            ClaimVO vo = new ClaimVO();
            vo.setClaimId(c.getId());
            vo.setId(it.getId());
            vo.setTitle(it.getTitle());
            vo.setImg(it.getCoverImg());
            vo.setLocation(it.getLocation());
            vo.setTime(c.getCreateTime() != null ? c.getCreateTime().format(OUT) : "");
            vo.setClaimStatus(c.getStatus());
            vo.setReply(c.getReply());
            vo.setProof(c.getProof());
            vo.setContact(c.getContact());
            User claimant = userMap.get(c.getClaimantId());
            if (claimant != null) {
                vo.setClaimantName(claimant.getNickname());
                vo.setClaimantAccount(claimant.getAccount());
            }
            result.add(vo);
        }
        return result;
    }

    private void push(Long userId, String type, String title, String content, Long refId) {
        Message m = new Message();
        m.setUserId(userId);
        m.setType(type);
        m.setTitle(title);
        m.setContent(content);
        m.setRefId(refId);
        m.setIsRead(0);
        messageMapper.insert(m);
    }

    private String blankTo(String s, String def) {
        return (s == null || s.isBlank()) ? def : s.trim();
    }
}
