package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiguang.lostfound.entity.Item;
import com.shiguang.lostfound.entity.User;
import com.shiguang.lostfound.mapper.ItemMapper;
import com.shiguang.lostfound.mapper.UserMapper;
import com.shiguang.lostfound.vo.StatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计服务。
 * 审查 2.30 修正：首页 lostCount/foundCount 只统计"仍在寻找/待认领"的（排除已找回/已认领），
 * 使"寻找中"计数与墙上条数对得上；reunited 仍为累计已找回/已认领。
 * 审查 2.8/附录E5：dashboard 补 userCount 与近 30 天 dailyTrend。
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final ItemMapper itemMapper;
    private final UserMapper userMapper;

    private static final List<String> CLOSED = List.of("已找回", "已认领", "已下架", "已关闭");

    public StatsVO homeStats() {
        StatsVO s = new StatsVO();
        s.setReunited(itemMapper.countReunited());
        s.setLostCount(itemMapper.selectCount(new QueryWrapper<Item>()
                .eq("type", "lost").eq("audit_status", 1).notIn("status", CLOSED)));
        s.setFoundCount(itemMapper.selectCount(new QueryWrapper<Item>()
                .eq("type", "found").eq("audit_status", 1).notIn("status", CLOSED)));
        return s;
    }

    public List<Map<String, Object>> categoryRatio() {
        return itemMapper.countByCategory();
    }

    /** 管理后台看板：总数/团聚/成功率/分类占比/用户总数/近30天趋势。 */
    public Map<String, Object> dashboard() {
        long total = itemMapper.selectCount(new QueryWrapper<Item>().eq("audit_status", 1));
        long reunited = itemMapper.countReunited();
        double rate = total == 0 ? 0 : Math.round(reunited * 1000.0 / total) / 10.0;
        Map<String, Object> m = new HashMap<>();
        m.put("total", total);
        m.put("reunited", reunited);
        m.put("successRate", rate);
        m.put("categoryRatio", categoryRatio());
        m.put("userCount", userMapper.selectCount(new QueryWrapper<User>()));
        m.put("dailyTrend", itemMapper.dailyTrend(LocalDateTime.now().minusDays(30)));
        return m;
    }
}
