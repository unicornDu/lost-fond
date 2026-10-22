package com.shiguang.lostfound.service;

import com.shiguang.lostfound.mapper.ItemMapper;
import com.shiguang.lostfound.mapper.UserMapper;
import com.shiguang.lostfound.vo.StatsVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Z-04：统计服务单元测试（Mockito，不连库）。
 * 锚定 B-02 口径：homeStats 只统计 audit=1 且未团聚的；dashboard 成功率按 total 计算。
 */
@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock ItemMapper itemMapper;
    @Mock UserMapper userMapper;

    private StatsService newService() {
        return new StatsService(itemMapper, userMapper);
    }

    @Test
    @DisplayName("homeStats：reunited/lostCount/foundCount 三口径（B-02）")
    void homeStatsReturnsThreeNumbers() {
        when(itemMapper.countReunited()).thenReturn(5L);
        when(itemMapper.selectCount(any())).thenReturn(10L, 3L);   // lostCount、foundCount 依次

        StatsVO s = newService().homeStats();
        assertEquals(5L, s.getReunited());
        assertEquals(10L, s.getLostCount());
        assertEquals(3L, s.getFoundCount());
        verify(itemMapper, times(2)).selectCount(any());
    }

    @Test
    @DisplayName("dashboard：总数/团聚/成功率/用户数/趋势齐全")
    void dashboardAssemblesAllFields() {
        when(itemMapper.selectCount(any())).thenReturn(20L);
        when(itemMapper.countReunited()).thenReturn(8L);
        when(userMapper.selectCount(any())).thenReturn(15L);
        when(itemMapper.countByCategory()).thenReturn(List.of());
        when(itemMapper.dailyTrend(any())).thenReturn(List.of());

        Map<String, Object> m = newService().dashboard();
        assertEquals(20L, m.get("total"));
        assertEquals(8L, m.get("reunited"));
        assertEquals(40.0, m.get("successRate"));
        assertEquals(15L, m.get("userCount"));
        assertNotNull(m.get("categoryRatio"));
        assertNotNull(m.get("dailyTrend"));
    }

    @Test
    @DisplayName("dashboard：total=0 时成功率为 0（防除零）")
    void dashboardZeroTotalSafe() {
        when(itemMapper.selectCount(any())).thenReturn(0L);
        when(itemMapper.countReunited()).thenReturn(0L);
        when(userMapper.selectCount(any())).thenReturn(1L);
        when(itemMapper.countByCategory()).thenReturn(List.of());
        when(itemMapper.dailyTrend(any())).thenReturn(List.of());

        assertEquals(0.0, newService().dashboard().get("successRate"));
    }
}
