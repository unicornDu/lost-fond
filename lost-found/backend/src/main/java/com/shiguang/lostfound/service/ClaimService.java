package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiguang.lostfound.common.BusinessException;
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
 *  - apply() 白名单守卫：仅「寻找中/待认领」可申请（已下架/已关闭/已完成不可）。
 *  - audit() 通过：物品置 已找回/已认领；同物品其余「审核中」申请批量置「已拒绝」并逐个通知。
 *  - audit() 拒绝：仅当同物品已无其他「审核中」申请时才回退物品状态（避免状态自相矛盾）。
 *  - audit() 守卫：物品已完成认领 / 申请已处理过 / 非发布者 → 拒绝。
 * 审查 2.5：ClaimVO 回填申请人昵称/学号/凭证/联系方式，杜绝盲审。
 * 桌面端 P1：审核通过即生成认领证明 PDF，交付失主与认领人各一份（失败仅记日志，不阻断审核主流程）。
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

    /** 审查 2.6 / 附录 D4：完整审核逻辑。 */
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
            c.setStatus("已通过");
            c.setReply(blankTo(req.getReply(), "已通过认领，请联系交接"));
            claimMapper.updateById(c);

            item.setStatus(lost ? "已找回" : "已认领");
            itemMapper.updateById(item);

            // 关闭同物品其余「审核中」申请并逐个通知
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

            // 桌面端 P1：生成认领证明 PDF 交付双方（失败仅记日志——证明是增值产物，不能让审核回滚）
            try {
                User owner = userMapper.selectById(item.getUserId());
                User claimant = userMapper.selectById(c.getClaimantId());
                byte[] pdf = pdfService.claimCertificate(item, c, owner, claimant);
                String certTitle = "认领证明 #" + c.getId();
                String desc = "物品「" + item.getTitle() + "」认领成功交接凭证";
                String fileName = "认领证明-" + String.format("%06d", c.getId()) + ".pdf";
                fileDeliveryService.deliver(item.getUserId(), "CLAIM_CERT", item.getId(),
                        certTitle, desc, pdf, "pdf", "application/pdf");
                fileDeliveryService.deliver(c.getClaimantId(), "CLAIM_CERT", item.getId(),
                        certTitle, desc, pdf, "pdf", "application/pdf");
            } catch (Exception e) {
                log.warn("认领证明生成/交付失败（不阻断审核）: claimId={}", c.getId(), e);
            }
        } else {
            c.setStatus("已拒绝");
            c.setReply(blankTo(req.getReply(), "特征不符，已拒绝"));
            claimMapper.updateById(c);   // 先落库，下面的计数才会排除自身

            // 仅当已无其他「审核中」申请时才回退物品状态
            Long pending = claimMapper.selectCount(new QueryWrapper<ClaimRecord>()
                    .eq("item_id", c.getItemId()).eq("status", "审核中"));
            if (pending == null || pending == 0) {
                item.setStatus(lost ? "寻找中" : "待认领");
                itemMapper.updateById(item);
            }
            push(c.getClaimantId(), "claim", "认领申请未通过",
                    "你对「" + item.getTitle() + "」的认领申请未通过：" + c.getReply(), item.getId());
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
