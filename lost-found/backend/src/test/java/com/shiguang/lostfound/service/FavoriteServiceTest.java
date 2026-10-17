package com.shiguang.lostfound.service;

import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.entity.Item;
import com.shiguang.lostfound.mapper.FavoriteMapper;
import com.shiguang.lostfound.mapper.ItemMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * E-07：收藏服务单测（Mockito，不连库）。
 * 覆盖：正常收藏 / 重复收藏幂等（不重复插入）/ 不可见启事 400 / 不存在 404 /
 * 取消幂等 / 删除启事级联清理。
 */
@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock FavoriteMapper favoriteMapper;
    @Mock ItemMapper itemMapper;
    @Mock ItemService itemService;

    private FavoriteService service() {
        return new FavoriteService(favoriteMapper, itemMapper, itemService);
    }

    private Item item(String status, int audit) {
        Item it = new Item();
        it.setId(1L);
        it.setUserId(9L);
        it.setStatus(status);
        it.setAuditStatus(audit);
        it.setType("lost");
        return it;
    }

    @Test
    @DisplayName("add：公开启事可收藏")
    void addOk() {
        when(itemMapper.selectById(1L)).thenReturn(item("寻找中", 1));
        when(favoriteMapper.selectCount(any())).thenReturn(0L);
        assertDoesNotThrow(() -> service().add(5L, 1L));
        verify(favoriteMapper).insert(any(com.shiguang.lostfound.entity.Favorite.class));
    }

    @Test
    @DisplayName("add：重复收藏幂等，不重复插入")
    void addIdempotent() {
        when(itemMapper.selectById(1L)).thenReturn(item("寻找中", 1));
        when(favoriteMapper.selectCount(any())).thenReturn(1L);
        assertDoesNotThrow(() -> service().add(5L, 1L));
        verify(favoriteMapper, never()).insert(any(com.shiguang.lostfound.entity.Favorite.class));
    }

    @Test
    @DisplayName("add：已下架启事 → 400（与公开可见口径一致）")
    void addRejectsHidden() {
        when(itemMapper.selectById(1L)).thenReturn(item("已下架", 1));
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service().add(5L, 1L)).getCode());
        verify(favoriteMapper, never()).insert(any(com.shiguang.lostfound.entity.Favorite.class));
    }

    @Test
    @DisplayName("add：待审核启事 → 400")
    void addRejectsPendingAudit() {
        when(itemMapper.selectById(1L)).thenReturn(item("寻找中", 0));
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service().add(5L, 1L)).getCode());
    }

    @Test
    @DisplayName("add：启事不存在 → 404")
    void addRejectsMissing() {
        when(itemMapper.selectById(1L)).thenReturn(null);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service().add(5L, 1L)).getCode());
    }

    @Test
    @DisplayName("remove：无该行也返回成功（幂等）")
    void removeIdempotent() {
        assertDoesNotThrow(() -> service().remove(5L, 1L));
        verify(favoriteMapper).delete(any());
    }

    @Test
    @DisplayName("deleteByItem：删除启事时按 item_id 清理收藏行")
    void cascadeDelete() {
        service().deleteByItem(1L);
        verify(favoriteMapper).delete(any());
    }
}
