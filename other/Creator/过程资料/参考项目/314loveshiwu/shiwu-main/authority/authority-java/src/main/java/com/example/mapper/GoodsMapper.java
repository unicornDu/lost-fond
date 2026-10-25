package com.example.mapper;

import com.example.entity.Goods;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 失物管理 Mapper 接口
 * </p>
 *
 * @author ren
 * @since 2023-02-13
 */
@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

}
