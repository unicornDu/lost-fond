package com.shiguang.lostfound.service;

import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.context.UserContext;
import com.shiguang.lostfound.dto.ItemForm;
import com.shiguang.lostfound.entity.ClaimRecord;
import com.shiguang.lostfound.entity.Item;
import com.shiguang.lostfound.mapper.ClaimRecordMapper;
import com.shiguang.lostfound.mapper.ItemImageMapper;
import com.shiguang.lostfound.mapper.ItemMapper;
import com.shiguang.lostfound.mapper.MessageMapper;
import com.shiguang.lostfound.mapper.UserMapper;
import com.shiguang.lostfound.util.PHashUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 05 A-03：发布守卫单元测试（Mockito，不连库）。
 * 覆盖：无图 400、图片超 3 张 400（B-10 上限，且超限时不产生任何磁盘写入）。
 * Z-02：markFound 状态白名单守卫 + "认领申请中"批量关闭在审申请。
 * Z-04/修正 E：detail 可见性判定（B-01 的单元级锚点）。
 */
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock ItemMapper itemMapper;
    @Mock ItemImageMapper itemImageMapper;
    @Mock UserMapper userMapper;
    @Mock MessageMapper messageMapper;
    @Mock ClaimRecordMapper claimRecordMapper;
    @Mock FileStorageService fileStorage;
    @Mock PHashUtil pHashUtil;

    @AfterEach
    void cleanContext() {
        UserContext.clear();   // detail 用例会写 ThreadLocal，不清会污染同线程后续用例
    }

    private ItemService newService() {
        return new ItemService(itemMapper, itemImageMapper, userMapper, messageMapper,
                claimRecordMapper, fileStorage, pHashUtil);
    }

    private ItemForm form() {
        ItemForm f = new ItemForm();
        f.setType("lost");
        f.setTitle("测试标题");
        f.setCategory("其他");
        f.setLocation("图书馆");
        f.setHappenTime("2026-10-14 09:00:00");
        f.setDescription("desc");
        return f;
    }

    private Item item(long id, long owner, String status) {
        Item it = new Item();
        it.setId(id);
        it.setUserId(owner);
        it.setStatus(status);
        it.setType("lost");
        it.setTitle("测试启事");
        it.setAuditStatus(1);
        return it;
    }

    // ---------------- publish（05 A-03） ----------------

    @Test
    @DisplayName("publish：无图被拒（审查 2.23）")
    void publishRejectsNoImage() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> newService().publish(1L, form(), new MultipartFile[0]));
        assertTrue(e.getMessage().contains("图片"));
        verify(itemMapper, never()).insert(any(Item.class));
    }

    @Test
    @DisplayName("publish：超过 3 张图被拒且不落盘（B-10）")
    void publishRejectsTooManyImages() {
        MultipartFile[] files = new MultipartFile[4];
        for (int i = 0; i < 4; i++) files[i] = mock(MultipartFile.class);
        BusinessException e = assertThrows(BusinessException.class,
                () -> newService().publish(1L, form(), files));
        assertTrue(e.getMessage().contains("3 张"));
        verifyNoInteractions(fileStorage);   // 上限校验在读字节/落盘之前
    }

    // ---------------- markFound（Z-02） ----------------

    @Test
    @DisplayName("markFound：本人+寻找中 → 成功写终态")
    void markFoundOkWhenActive() {
        when(itemMapper.selectById(1L)).thenReturn(item(1L, 9L, "寻找中"));
        when(claimRecordMapper.selectList(any())).thenReturn(List.of());
        assertDoesNotThrow(() -> newService().markFound(1L, 9L));
        verify(itemMapper).updateById(any(Item.class));
        verify(messageMapper, never()).insert(any(com.shiguang.lostfound.entity.Message.class));
    }

    @Test
    @DisplayName("markFound：非本人 → 403")
    void markFoundRejectsOthers() {
        when(itemMapper.selectById(1L)).thenReturn(item(1L, 9L, "寻找中"));
        assertEquals("只能操作自己发布的启事",
                assertThrows(BusinessException.class, () -> newService().markFound(1L, 8L)).getMessage());
        verify(itemMapper, never()).updateById(any(Item.class));
    }

    @Test
    @DisplayName("markFound：已找回（终态）→ 400")
    void markFoundRejectsDoneLost() {
        when(itemMapper.selectById(1L)).thenReturn(item(1L, 9L, "已找回"));
        assertTrue(assertThrows(BusinessException.class, () -> newService().markFound(1L, 9L))
                .getMessage().contains("不允许标记完成"));
    }

    @Test
    @DisplayName("markFound：已下架 → 400（管理端处置不可被翻回终态）")
    void markFoundRejectsOffline() {
        when(itemMapper.selectById(1L)).thenReturn(item(1L, 9L, "已下架"));
        assertTrue(assertThrows(BusinessException.class, () -> newService().markFound(1L, 9L))
                .getMessage().contains("不允许标记完成"));
    }

    @Test
    @DisplayName("markFound：认领申请中 → 成功且批量关闭在审申请并通知（悬挂申请修复）")
    void markFoundClosesPendingClaims() {
        when(itemMapper.selectById(1L)).thenReturn(item(1L, 9L, "认领申请中"));
        ClaimRecord pending = new ClaimRecord();
        pending.setId(7L);
        pending.setItemId(1L);
        pending.setClaimantId(5L);
        pending.setStatus("审核中");
        when(claimRecordMapper.selectList(any())).thenReturn(List.of(pending));

        assertDoesNotThrow(() -> newService().markFound(1L, 9L));

        assertEquals("已拒绝", pending.getStatus());
        assertTrue(pending.getReply().contains("自动关闭"));
        verify(claimRecordMapper).updateById(pending);
        verify(messageMapper).insert(any(com.shiguang.lostfound.entity.Message.class));
    }

    // ---------------- detail 可见性（Z-04 修正 E，B-01 锚点） ----------------

    @Test
    @DisplayName("detail：匿名 + 待审启事 → 404（B-01 防探测）")
    void detailHidesPendingFromAnonymous() {
        Item it = item(5L, 9L, "寻找中");
        it.setAuditStatus(0);
        when(itemMapper.selectById(5L)).thenReturn(it);
        assertEquals("启事不存在或已被删除",
                assertThrows(BusinessException.class, () -> newService().detail(5L, null, "k")).getMessage());
    }

    @Test
    @DisplayName("detail：本人 + 待审启事 → 可见")
    void detailAllowsOwnerForPending() {
        Item it = item(5L, 9L, "寻找中");
        it.setAuditStatus(0);
        stubDetailJoins();
        when(itemMapper.selectById(5L)).thenReturn(it);
        assertNotNull(newService().detail(5L, 9L, "k"));
    }

    @Test
    @DisplayName("detail：管理员 + 待审启事 → 可见")
    void detailAllowsAdminForPending() {
        Item it = item(5L, 9L, "寻找中");
        it.setAuditStatus(0);
        stubDetailJoins();
        when(itemMapper.selectById(5L)).thenReturn(it);
        UserContext.set(1L, 1);
        assertNotNull(newService().detail(5L, null, "k"));
    }

    @Test
    @DisplayName("detail：匿名 + 已过审启事 → 可见")
    void detailShowsApprovedToAnonymous() {
        stubDetailJoins();
        when(itemMapper.selectById(5L)).thenReturn(item(5L, 9L, "寻找中"));
        assertNotNull(newService().detail(5L, null, "k"));
    }

    @Test
    @DisplayName("detail：匿名 + 已下架启事 → 404")
    void detailHidesOfflineFromAnonymous() {
        when(itemMapper.selectById(5L)).thenReturn(item(5L, 9L, "已下架"));
        assertEquals("启事不存在或已被删除",
                assertThrows(BusinessException.class, () -> newService().detail(5L, null, "k")).getMessage());
    }

    private void stubDetailJoins() {
        when(itemImageMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectBatchIds(any())).thenReturn(List.of());
    }
}
