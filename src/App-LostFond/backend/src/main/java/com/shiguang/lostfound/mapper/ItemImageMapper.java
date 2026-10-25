package com.shiguang.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiguang.lostfound.entity.ItemImage;
import org.apache.ibatis.annotations.Mapper;

/**
 * App 独立版：pHash 汉明距离改在 SearchService 里用 Long.bitCount 计算，
 * 不再依赖 MySQL 的 BIT_COUNT / ^ 语法，同一份代码可跑 MySQL 与内置 H2。
 */
@Mapper
public interface ItemImageMapper extends BaseMapper<ItemImage> {
}
