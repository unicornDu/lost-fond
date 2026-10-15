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

    /** 已找回/已认领总数（首页统计）。 */
    @Select("SELECT COUNT(*) FROM item WHERE status IN ('已找回','已认领')")
    long countReunited();
}
