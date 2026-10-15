package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.config.ImageSearchProperties;
import com.shiguang.lostfound.dto.ImageMatch;
import com.shiguang.lostfound.entity.Item;
import com.shiguang.lostfound.mapper.ItemImageMapper;
import com.shiguang.lostfound.mapper.ItemMapper;
import com.shiguang.lostfound.util.PHashUtil;
import com.shiguang.lostfound.vo.ItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 05 A-03：以图搜图核心链路单元测试（Mockito，不连库）。
 * 覆盖：审核/下架过滤、similarTo 排除自身、n<=0 返回空、不可见源返回空、上传前置校验被调用。
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock ItemImageMapper itemImageMapper;
    @Mock ItemMapper itemMapper;
    @Mock PHashUtil pHashUtil;
    @Mock ItemService itemService;
    @Mock FileStorageService fileStorage;

    private SearchService svc;

    @BeforeEach
    void setUp() {
        ImageSearchProperties props = new ImageSearchProperties();
        props.setBitResolution(64);
        props.setHammingThreshold(12);
        props.setTopN(10);
        svc = new SearchService(itemImageMapper, itemMapper, pHashUtil, props, itemService, fileStorage);
    }

    private Item item(long id, int audit, String status) {
        Item it = new Item();
        it.setId(id);
        it.setUserId(1L);
        it.setAuditStatus(audit);
        it.setStatus(status);
        return it;
    }

    @Test
    @DisplayName("searchByHash：待审核与已下架项被过滤（第二道防线）")
    void filtersUnauditedAndHidden() {
        when(itemImageMapper.searchByHash(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(m(1L, 0), m(2L, 1), m(3L, 2)));
        when(itemMapper.selectBatchIds(any()))
                .thenReturn(List.of(item(1, 1, "寻找中"), item(2, 0, "寻找中"), item(3, 1, "已下架")));
        when(itemService.loadNicknames(any())).thenReturn(Map.of(1L, "甲"));
        when(itemService.toVO(any(), any(), any())).thenAnswer(inv -> {
            ItemVO vo = new ItemVO();
            vo.setId(((Item) inv.getArgument(0)).getId());
            return vo;
        });
        List<ItemVO> res = svc.searchByHash(123L);
        assertEquals(1, res.size());
        assertEquals(1L, res.get(0).getId());
    }

    @Test
    @DisplayName("searchByImage：先做扩展名/魔数前置校验（T-04）")
    void searchByImageValidatesFirst() throws Exception {
        MultipartFile f = mock(MultipartFile.class);
        when(f.isEmpty()).thenReturn(false);
        when(f.getOriginalFilename()).thenReturn("a.txt");
        when(f.getBytes()).thenReturn(new byte[]{1, 2, 3});
        doThrow(new BusinessException("仅支持 jpg/png/gif/bmp/webp 图片"))
                .when(fileStorage).validateImage("a.txt", new byte[]{1, 2, 3});
        assertThrows(BusinessException.class, () -> svc.searchByImage(f));
        verify(fileStorage).validateImage("a.txt", new byte[]{1, 2, 3});
        verify(pHashUtil, never()).hash(any(byte[].class));
    }

    @Test
    @DisplayName("similarTo：n<=0 返回空（T-04 语义修复）")
    void similarToZeroReturnsEmpty() {
        assertTrue(svc.similarTo(1L, 0).isEmpty());
        assertTrue(svc.similarTo(1L, -3).isEmpty());
        verify(itemImageMapper, never()).searchByHash(anyLong(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("similarTo：源启事待审核时返回空（B-01 回归）")
    void similarToInvisibleSourceEmpty() {
        when(itemMapper.selectById(9L)).thenReturn(item(9, 0, "寻找中"));
        assertTrue(svc.similarTo(9L, 3).isEmpty());
        verify(itemImageMapper, never()).searchByHash(anyLong(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("similarTo：结果排除自身")
    void similarToExcludesSelf() {
        when(itemMapper.selectById(1L)).thenReturn(item(1, 1, "寻找中"));
        when(itemImageMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(img(1L, 11L), img(2L, 1L)));
        when(itemImageMapper.searchByHash(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(m(1L, 0), m(2L, 3)));
        when(itemMapper.selectBatchIds(any())).thenReturn(List.of(item(1, 1, "寻找中"), item(2, 1, "寻找中")));
        when(itemService.loadNicknames(any())).thenReturn(Map.of(1L, "甲"));
        when(itemService.toVO(any(), any(), any())).thenAnswer(inv -> {
            ItemVO vo = new ItemVO();
            vo.setId(((Item) inv.getArgument(0)).getId());
            return vo;
        });
        List<ItemVO> res = svc.similarTo(1L, 5);
        assertEquals(1, res.size());
        assertEquals(2L, res.get(0).getId());
    }

    private ImageMatch m(long itemId, int dist) {
        ImageMatch im = new ImageMatch();
        im.setItemId(itemId);
        im.setDist(dist);
        return im;
    }

    private com.shiguang.lostfound.entity.ItemImage img(long id, long phash) {
        com.shiguang.lostfound.entity.ItemImage im = new com.shiguang.lostfound.entity.ItemImage();
        im.setId(id);
        im.setPhash(phash);
        return im;
    }
}
