package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.entity.Item;
import com.shiguang.lostfound.entity.User;
import com.shiguang.lostfound.mapper.AdminLogMapper;
import com.shiguang.lostfound.mapper.ClaimRecordMapper;
import com.shiguang.lostfound.mapper.ItemImageMapper;
import com.shiguang.lostfound.mapper.ItemMapper;
import com.shiguang.lostfound.mapper.MessageMapper;
import com.shiguang.lostfound.mapper.UserMapper;
import com.shiguang.lostfound.vo.ItemVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Z-04：管理后台单元测试（Mockito，不连库）。此前 AdminService 零单测——
 * 审核状态机（B-03 守卫）、删除连带清理（B-05）、封禁边界全靠 test_api.py 黑盒兜底。
 * Z-10：offlineItem 已完成守卫的回归锚点。
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock UserMapper userMapper;
    @Mock ItemMapper itemMapper;
    @Mock ItemImageMapper itemImageMapper;
    @Mock ClaimRecordMapper claimMapper;
    @Mock MessageMapper messageMapper;
    @Mock AdminLogMapper adminLogMapper;
    @Mock ItemService itemService;
    @Mock FileStorageService fileStorage;
    @Mock FavoriteService favoriteService;
    @Mock com.shiguang.lostfound.mapper.FavoriteMapper favoriteMapper;
    @Mock FileDeliveryService fileDeliveryService;                              // P1：交付体系
    @Mock com.shiguang.lostfound.mapper.DeliveryMapper deliveryMapper;
    @Mock WatchService watchService;                                            // v2.20：守望反查

    private AdminService newService() {
        return new AdminService(userMapper, itemMapper, itemImageMapper, claimMapper,
                messageMapper, adminLogMapper, itemService, fileStorage, favoriteService, favoriteMapper,
                fileDeliveryService, deliveryMapper, watchService);
    }

    private Item item(long id, String status, int audit) {
        Item it = new Item();
        it.setId(id);
        it.setUserId(9L);
        it.setStatus(status);
        it.setType("lost");
        it.setAuditStatus(audit);
        return it;
    }

    // ---------------- auditItem（B-03 状态机 + E-02 通知） ----------------

    @Test
    @DisplayName("auditItem：通过 → audit=1 且写日志、通知发布者（E-02）")
    void auditApproves() {
        when(itemMapper.selectById(1L)).thenReturn(item(1L, "寻找中", 0));
        assertDoesNotThrow(() -> newService().auditItem(1L, 1L, true, null));
        verify(itemMapper).updateById(any(Item.class));
        verify(adminLogMapper).insert(any(com.shiguang.lostfound.entity.AdminLog.class));
        verify(messageMapper).insert(argThat((com.shiguang.lostfound.entity.Message m) ->
                "audit".equals(m.getType()) && m.getUserId().equals(9L) && m.getRefId().equals(1L)));
    }

    @Test
    @DisplayName("auditItem：已完成（已认领）再审核被 B-03 守卫拒绝，且不发通知（E-02）")
    void auditRejectsCompleted() {
        when(itemMapper.selectById(1L)).thenReturn(item(1L, "已认领", 1));
        assertTrue(assertThrows(BusinessException.class, () -> newService().auditItem(1L, 1L, true, null))
                .getMessage().contains("已完成认领流程"));
        verify(itemMapper, never()).updateById(any(Item.class));
        verify(messageMapper, never()).insert(any(com.shiguang.lostfound.entity.Message.class));
    }

    @Test
    @DisplayName("auditItem：通过时历史「已关闭」恢复为业务初始状态（B-03）")
    void auditRevivesLegacyClosed() {
        Item it = item(1L, "已关闭", 2);
        when(itemMapper.selectById(1L)).thenReturn(it);
        newService().auditItem(1L, 1L, true, null);
        assertEquals("寻找中", it.getStatus());
        assertEquals(1, it.getAuditStatus());
    }

    // ---------------- E-02：驳回理由与通知内容 ----------------

    @Test
    @DisplayName("E-02：驳回带理由 → 通知 content 含「未通过审核」与理由")
    void auditRejectNotifiesWithReason() {
        when(itemMapper.selectById(1L)).thenReturn(item(1L, "寻找中", 1));
        newService().auditItem(1L, 1L, false, "图片模糊");
        verify(messageMapper).insert(argThat((com.shiguang.lostfound.entity.Message m) ->
                "审核未通过".equals(m.getTitle())
                        && m.getContent().contains("未通过审核") && m.getContent().contains("图片模糊")));
    }

    @Test
    @DisplayName("E-02：驳回无理由 → 通知用默认文案")
    void auditRejectDefaultReason() {
        when(itemMapper.selectById(1L)).thenReturn(item(1L, "寻找中", 1));
        newService().auditItem(1L, 1L, false, "  ");
        verify(messageMapper).insert(argThat((com.shiguang.lostfound.entity.Message m) ->
                m.getContent().contains("内容不符合发布规范")));
    }

    @Test
    @DisplayName("E-02：理由超 200 字 → 400 且不落任何数据")
    void auditRejectTooLongReason() {
        // 理由校验在最前，连 selectById 都不会发生（先校验后写库）
        assertEquals(400, assertThrows(BusinessException.class,
                () -> newService().auditItem(1L, 1L, false, "长".repeat(201))).getCode());
        verify(itemMapper, never()).selectById(anyLong());
        verify(itemMapper, never()).updateById(any(Item.class));
        verify(adminLogMapper, never()).insert(any(com.shiguang.lostfound.entity.AdminLog.class));
        verify(messageMapper, never()).insert(any(com.shiguang.lostfound.entity.Message.class));
    }

    // ---------------- offlineItem（Z-10 + E-02 通知） ----------------

    @Test
    @DisplayName("offlineItem：已团聚（已找回）→ 400 且数据不变、不发通知（Z-10 守卫）")
    void offlineRejectsCompleted() {
        when(itemMapper.selectById(1L)).thenReturn(item(1L, "已找回", 1));
        assertTrue(assertThrows(BusinessException.class, () -> newService().offlineItem(1L, 1L, null))
                .getMessage().contains("不允许下架"));
        verify(itemMapper, never()).updateById(any(Item.class));
        verify(messageMapper, never()).insert(any(com.shiguang.lostfound.entity.Message.class));
    }

    @Test
    @DisplayName("offlineItem：正常在流转启事 → 已下架 + audit=2 + 通知发布者（E-02）")
    void offlineMarksItem() {
        when(itemMapper.selectById(1L)).thenReturn(item(1L, "待认领", 1));
        assertDoesNotThrow(() -> newService().offlineItem(1L, 1L, "涉嫌违规"));
        verify(itemMapper).updateById(any(Item.class));
        verify(adminLogMapper).insert(any(com.shiguang.lostfound.entity.AdminLog.class));
        verify(messageMapper).insert(argThat((com.shiguang.lostfound.entity.Message m) ->
                "启事已下架".equals(m.getTitle()) && m.getContent().contains("涉嫌违规")));
    }

    // ---------------- setUserStatus（封禁边界） ----------------

    @Test
    @DisplayName("setUserStatus：不可封禁管理员")
    void banRejectsAdmin() {
        User admin = new User();
        admin.setId(2L);
        admin.setRole(1);
        when(userMapper.selectById(2L)).thenReturn(admin);
        assertEquals("不能封禁管理员",
                assertThrows(BusinessException.class, () -> newService().setUserStatus(1L, 2L, true)).getMessage());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("setUserStatus：封禁普通用户 → status=0 且写 admin_log")
    void bansNormalUser() {
        User u = new User();
        u.setId(3L);
        u.setRole(0);
        when(userMapper.selectById(3L)).thenReturn(u);
        assertDoesNotThrow(() -> newService().setUserStatus(1L, 3L, true));
        assertEquals(0, u.getStatus());
        verify(adminLogMapper).insert(any(com.shiguang.lostfound.entity.AdminLog.class));
    }

    // ---------------- deleteItem（B-05 连带清理） ----------------

    @Test
    @DisplayName("deleteItem：清 image/claim/message(ref_id 全清)/item + 删盘")
    void deleteCleansAllRelations() {
        when(itemMapper.selectById(1L)).thenReturn(item(1L, "寻找中", 1));
        com.shiguang.lostfound.entity.ItemImage img = new com.shiguang.lostfound.entity.ItemImage();
        img.setImgUrl("/uploads/2026/10/a.jpg");
        when(itemImageMapper.selectList(any())).thenReturn(List.of(img));

        assertDoesNotThrow(() -> newService().deleteItem(1L, 1L));

        verify(itemImageMapper).delete(any(QueryWrapper.class));
        verify(claimMapper).delete(any(QueryWrapper.class));
        verify(messageMapper).delete(any(QueryWrapper.class));   // B-05：不再限定 type，dm 一并清理
        verify(favoriteService).deleteByItem(1L);                // E-07：级联清理收藏行
        verify(fileDeliveryService).deleteByBiz(1L);             // P1：级联清理交付文件（行+盘）
        verify(itemMapper).deleteById(1L);
        verify(fileStorage).deleteQuietly("/uploads/2026/10/a.jpg");
        verify(adminLogMapper).insert(any(com.shiguang.lostfound.entity.AdminLog.class));
    }

    // ---------------- listItems / dbCounts ----------------

    @Test
    @DisplayName("listItems：按 auditStatus 过滤分页返回")
    void listItemsFilters() {
        Page<Item> p = new Page<>(1, 10);
        p.setRecords(List.of(item(1L, "寻找中", 1)));
        p.setTotal(1);
        when(itemMapper.selectPage(any(), any())).thenReturn(p);
        when(itemService.loadNicknames(any())).thenReturn(Map.of());
        when(itemService.toVO(any(), any(), any())).thenReturn(new ItemVO());

        var result = newService().listItems(1, 10, 1, null);
        assertEquals(1, result.getTotal());
        verify(itemMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("dbCounts：八表行数齐全（05 A-01 + E-07 favorite + P1 delivery）")
    void dbCountsReturnsSixTables() {
        when(userMapper.selectCount(any())).thenReturn(11L);
        when(itemMapper.selectCount(any())).thenReturn(22L);
        when(itemImageMapper.selectCount(any())).thenReturn(33L);
        when(claimMapper.selectCount(any())).thenReturn(44L);
        when(messageMapper.selectCount(any())).thenReturn(55L);
        when(favoriteMapper.selectCount(any())).thenReturn(56L);
        when(deliveryMapper.selectCount(any())).thenReturn(57L);
        when(adminLogMapper.selectCount(any())).thenReturn(66L);

        Map<String, Long> m = newService().dbCounts();
        assertEquals(8, m.size());
        assertEquals(22L, m.get("item"));
        assertEquals(56L, m.get("favorite"));
        assertEquals(57L, m.get("delivery"));
        assertEquals(66L, m.get("admin_log"));
    }

    // ---------------- P1：CSV 报表入 delivery 体系 ----------------

    @Test
    @DisplayName("P1：CSV 报表入 delivery（BOM + 公式前缀 ' 转义 + 交付给管理员本人）")
    void csvEscapesFormulaPrefix() {
        Item malicious = item(1L, "寻找中", 1);
        malicious.setTitle("=SUM(A1)");
        malicious.setLocation("@cmd");
        when(itemMapper.selectList(any())).thenReturn(List.of(malicious));
        when(itemService.loadNicknames(any())).thenReturn(Map.of(9L, "同学"));
        when(fileDeliveryService.deliver(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    com.shiguang.lostfound.entity.Delivery d = new com.shiguang.lostfound.entity.Delivery();
                    d.setId(99L);
                    d.setBizType(inv.getArgument(1));
                    return d;
                });

        newService().createCsvReport(7L);

        verify(fileDeliveryService).deliver(
                eq(7L), eq("REPORT_CSV"), isNull(),
                contains("启事数据报表"), any(),
                argThat((byte[] csv) -> {
                    String s = new String(csv, java.nio.charset.StandardCharsets.UTF_8);
                    return s.startsWith("\uFEFF")                                            // BOM
                            && s.contains("id,类型,名称,分类,地点,状态,审核状态,发布人,发布时间")
                            && s.contains(",'=SUM(A1),")                                     // = 前缀被转义
                            && s.contains(",'@cmd,");                                        // @ 前缀被转义
                }),
                eq("csv"), eq("text/csv"));
    }
}
