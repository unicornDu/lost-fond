package com.example.mapper;

import com.example.entity.Menu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author   
 * @since 2023-01-09
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    List<Menu> getmenuBydes();

}
