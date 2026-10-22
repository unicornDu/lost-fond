package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.dto.ClaimAuditReq;
import com.shiguang.lostfound.dto.ClaimReq;
import com.shiguang.lostfound.entity.ClaimRecord;
import com.shiguang.lostfound.entity.Item;
import com.shiguang.lostfound.mapper.ClaimRecordMapper;
import com.shiguang.lostfound.mapper.ItemMapper;
import com.shiguang.lostfound.mapper.MessageMapper;
import com.shiguang.lostfound.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 05 A-03：认领状态机单元测试（Mockito，不连库）。
 * 覆盖 09 缺陷表 #4 的回归：apply 白名单守卫 / 重复申请 / audit 已处理 / 已完成守卫 /
 * 通过时批量拒绝其余在审 / 拒绝时"仅当无其他在审才回退"。
 * 2026-10-18 审查修复：审核改为条件更新（CAS），新增两条并发守卫用例；
 * 认领证明改为事务后置（单测无事务 → 内联执行，不影响原有断言）。
 */
@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock ClaimRecordMapper claimMapper;
    @Mock ItemMapper itemMapper;
    @Mock MessageMapper messageMapper;
    @Mock UserMapper userMapper;
    @Mock ItemService itemService;
    @Mock com.shiguang.lostfound.service.PdfService pdfService;                 // P1：认领证明生成器
    @Mock FileDeliveryService fileDeliveryService;                              // P1：交付服务

    private ClaimService newService() {
        return new ClaimService(claimMapper, itemMapper, messageMapper, userMapper, itemService, pdfService, fileDeliveryService);
    }

    private Item item(long id, long owner, String status) {
        Item it = new Item();
        it.setId(id);
        it.setUserId(owner);
        it.setStatus(status);
        it.setType("lost");
        it.setTitle("测试启事");
        return it;
    }

    private ClaimRecord claim(long id, long itemId, long claimantId, String status) {
        ClaimRecord c = new ClaimRecord();
        c.setId(id);
        c.setItemId(itemId);
        c.setClaimantId(claimantId);
        c.setStatus(status);
        return c;
    }

    @Test
    @DisplayName("apply：不能认领自己发布的启事")
    void applyRejectsOwnItem() {
        when(itemMapper.selectById(1L)).thenReturn(item(1L, 9L, "寻找中"));
        ClaimReq req = new ClaimReq();
        req.setProof("p");
        req.setContact("c");
        assertEquals("不能认领自己发布的启事",
                assertThrows(BusinessException.class, () -> newService().apply(1L, 9L, req)).getMessage());
    }

    @Test
    @DisplayName("apply：白名单守卫——已认领状态不可申请")
    void applyRejectsFinishedStatus() {
        when(itemMapper.selectById(1L)).thenReturn(item(1L, 9L, "已认领"));
        ClaimReq req = new ClaimReq();
        req.setProof("p");
        req.setContact("c");
        assertTrue(assertThrows(BusinessException.class, () -> newService().apply(1L, 5L, req))
                .getMessage().contains("不可申请认领"));
    }

    @Test
    @DisplayName("apply：同人同物已有审核中申请被拒")
    void applyRejectsDuplicate() {
        when(itemMapper.selectById(1L)).thenReturn(item(1L, 9L, "寻找中"));
        when(claimMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        ClaimReq req = new ClaimReq();
        req.setProof("p");
        req.setContact("c");
        assertTrue(assertThrows(BusinessException.class, () -> newService().apply(1L, 5L, req))
                .getMessage().contains("已提交过"));
    }

    @Test
    @DisplayName("apply：成功路径写认领+改状态+发通知")
    void applySuccess() {
        when(itemMapper.selectById(1L)).thenReturn(item(1L, 9L, "寻找中"));
        when(claimMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        ClaimReq req = new ClaimReq();
        req.setProof("p");
        req.setContact("c");
        newService().apply(1L, 5L, req);
        verify(claimMapper).insert(any(ClaimRecord.class));
        verify(itemMapper).updateById(any(Item.class));
        verify(messageMapper).insert(any(com.shiguang.lostfound.entity.Message.class));
    }

    @Test
    @DisplayName("audit：非发布者审核 403")
    void auditRejectsNonPublisher() {
        when(claimMapper.selectById(7L)).thenReturn(claim(7L, 1L, 5L, "审核中"));
        when(itemMapper.selectById(1L)).thenReturn(item(1L, 9L, "认领申请中"));
        ClaimAuditReq req = new ClaimAuditReq();
        req.setApprove(true);
        BusinessException e = assertThrows(BusinessException.class, () -> newService().audit(7L, 5L, req));
        assertEquals(403, e.getCode());
    }

    @Test
    @DisplayName("audit：已处理过的申请再次审核被拒")
    void auditRejectsProcessed() {
        when(claimMapper.selectById(7L)).thenReturn(claim(7L, 1L, 5L, "已通过"));
        when(itemMapper.selectById(1L)).thenReturn(item(1L, 9L, "已找回"));
        ClaimAuditReq req = new ClaimAuditReq();
        req.setApprove(true);
        assertTrue(assertThrows(BusinessException.class, () -> newService().audit(7L, 9L, req))
                .getMessage().contains("已处理过"));
    }

    @Test
    @DisplayName("audit：物品已完成认领时守卫拒绝")
    void auditRejectsCompletedItem() {
        when(claimMapper.selectById(7L)).thenReturn(claim(7L, 1L, 5L, "审核中"));
        when(itemMapper.selectById(1L)).thenReturn(item(1L, 9L, "已认领"));
        ClaimAuditReq req = new ClaimAuditReq();
        req.setApprove(true);
        assertTrue(assertThrows(BusinessException.class, () -> newService().audit(7L, 9L, req))
                .getMessage().contains("已完成认领"));
    }

    @Test
    @DisplayName("audit：通过时批量拒绝同物品其余在审申请并通知（缺陷#4 回归）")
    void auditApproveClosesOthers() {
        ClaimRecord c = claim(7L, 1L, 5L, "审核中");
        ClaimRecord other = claim(8L, 1L, 6L, "审核中");
        when(claimMapper.selectById(7L)).thenReturn(c);
        when(itemMapper.selectById(1L)).thenReturn(item(1L, 9L, "认领申请中"));
        when(claimMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(other));
        when(itemMapper.update(any(), any())).thenReturn(1);   // 物品 CAS 命中
        when(claimMapper.update(any(), any())).thenReturn(1);  // 申请 CAS 命中

        ClaimAuditReq req = new ClaimAuditReq();
        req.setApprove(true);
        newService().audit(7L, 9L, req);

        assertEquals("已拒绝", other.getStatus());
        verify(claimMapper).update(any(), any());              // 主申请：条件更新
        verify(claimMapper).updateById(any(ClaimRecord.class)); // 其余申请：逐条关闭
        verify(messageMapper, times(2)).insert(any(com.shiguang.lostfound.entity.Message.class)); // other + c
    }

    @Test
    @DisplayName("audit：并发下物品已被置终态 → 条件更新 0 行，抛业务异常（2026-10-18 新增）")
    void auditApproveRejectsWhenItemAlreadyClosed() {
        when(claimMapper.selectById(7L)).thenReturn(claim(7L, 1L, 5L, "审核中"));
        when(itemMapper.selectById(1L)).thenReturn(item(1L, 9L, "认领申请中"));
        when(itemMapper.update(any(), any())).thenReturn(0);   // 另一个事务已抢先完成

        ClaimAuditReq req = new ClaimAuditReq();
        req.setApprove(true);
        assertTrue(assertThrows(BusinessException.class, () -> newService().audit(7L, 9L, req))
                .getMessage().contains("已完成认领或状态已变更"));
        verify(claimMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("audit：并发下申请已被处理 → 申请 CAS 0 行，抛业务异常（2026-10-18 新增）")
    void auditApproveRejectsWhenClaimAlreadyProcessed() {
        when(claimMapper.selectById(7L)).thenReturn(claim(7L, 1L, 5L, "审核中"));
        when(itemMapper.selectById(1L)).thenReturn(item(1L, 9L, "认领申请中"));
        when(itemMapper.update(any(), any())).thenReturn(1);
        when(claimMapper.update(any(), any())).thenReturn(0);

        ClaimAuditReq req = new ClaimAuditReq();
        req.setApprove(true);
        assertTrue(assertThrows(BusinessException.class, () -> newService().audit(7L, 9L, req))
                .getMessage().contains("已处理过"));
    }

    @Test
    @DisplayName("audit：拒绝时仅当无其他在审才回退物品状态（缺陷#4 回归）")
    void auditRejectRollbackOnlyWhenNoPending() {
        // 场景1：无其他在审 → 回退（条件更新）
        ClaimRecord c = claim(7L, 1L, 5L, "审核中");
        when(claimMapper.selectById(7L)).thenReturn(c);
        when(itemMapper.selectById(1L)).thenReturn(item(1L, 9L, "认领申请中"));
        when(claimMapper.update(any(), any())).thenReturn(1);
        when(claimMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(itemMapper.update(any(), any())).thenReturn(1);

        ClaimAuditReq req = new ClaimAuditReq();
        req.setApprove(false);
        newService().audit(7L, 9L, req);
        verify(itemMapper).update(any(), any());   // 回退走了条件更新

        // 场景2：仍有其他在审 → 不回退
        ClaimRecord c2 = claim(9L, 2L, 5L, "审核中");
        when(claimMapper.selectById(9L)).thenReturn(c2);
        when(itemMapper.selectById(2L)).thenReturn(item(2L, 9L, "认领申请中"));
        when(claimMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        newService().audit(9L, 9L, req);
        verify(itemMapper, times(1)).update(any(), any());   // 场景2 不再更新物品
    }
}
