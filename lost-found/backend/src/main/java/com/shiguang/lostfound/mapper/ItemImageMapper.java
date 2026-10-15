package com.shiguang.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiguang.lostfound.dto.ImageMatch;
import com.shiguang.lostfound.entity.ItemImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ItemImageMapper extends BaseMapper<ItemImage> {

    /**
     * 以图搜图核心查询：用 64 位 pHash 计算汉明距离，按启事取最小距离，
     * 阈值过滤后升序返回 Top-N。
     * 注意①：MySQL 中 ^ 才是按位异或（XOR 关键字是逻辑异或，只返回 0/1）。
     * 注意②（审查 2.4 连带缺陷）：审核/下架过滤必须放进 SQL（JOIN item），
     * 否则 LIMIT 会先被"待审核/已下架"的匹配项占满，导致通过审核的结果被截断丢失。
     */
    @Select("""
            SELECT ii.item_id AS itemId, MIN(BIT_COUNT(ii.phash ^ #{q})) AS dist
            FROM item_image ii
            JOIN item i ON i.id = ii.item_id
            WHERE ii.phash IS NOT NULL
              AND i.audit_status = 1
              AND i.status NOT IN ('已下架', '已关闭')
            GROUP BY ii.item_id
            HAVING dist <= #{threshold}
            ORDER BY dist ASC
            LIMIT #{limit}
            """)
    List<ImageMatch> searchByHash(@Param("q") long q,
                                  @Param("threshold") int threshold,
                                  @Param("limit") int limit);
}
