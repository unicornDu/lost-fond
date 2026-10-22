package com.shiguang.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiguang.lostfound.entity.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ItemMapper extends BaseMapper<Item> {

    /** 审查 2.32：浏览量原子自增（避免读-改-写并发丢更新）。 */
    @Update("UPDATE item SET views = views + 1 WHERE id = #{id}")
    int incrementViews(@Param("id") Long id);

    /** 按分类统计启事数量（管理后台看板：分类占比）。 */
    @Select("SELECT category AS name, COUNT(*) AS value FROM item WHERE audit_status = 1 GROUP BY category")
    List<Map<String, Object>> countByCategory();

    /** 审查 2.8/附录E3：近 N 天按日发布趋势（看板折线图）。注意 @Param 必填。 */
    @Select("SELECT DATE(create_time) AS date, COUNT(*) AS value FROM item " +
            "WHERE create_time >= #{since} GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> dailyTrend(@Param("since") LocalDateTime since);

    /** 已找回/已认领总数（首页统计）。修复 B-02：必须过滤 audit_status=1，
     *  否则未过审数据会混进首页"已团聚"数字（实测曾多出 4 条待审核）。 */
    @Select("SELECT COUNT(*) FROM item WHERE audit_status = 1 AND status IN ('已找回','已认领')")
    long countReunited();

    /**
     * v2.41.1 首页活动总览：按"发生日"统计每日启事量（含寻物与招领，只算已过审）。
     * 用 happen_time 而不是 create_time——演示数据是一次性播种的，create_time 全挤在同一天，
     * 画出来只有一根柱子；happen_time 才是"那天真实发生了多少件失物/招领"，分布也有意义。
     * 只返回有数据的日期，空档由前端补全（后端不做日历填充）。
     */
    @Select("SELECT DATE(happen_time) AS date, COUNT(*) AS value FROM item " +
            "WHERE audit_status = 1 AND happen_time IS NOT NULL AND happen_time >= #{since} " +
            "GROUP BY DATE(happen_time) ORDER BY date")
    List<Map<String, Object>> dailyActivity(@Param("since") LocalDateTime since);
}
