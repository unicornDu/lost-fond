package com.shiguang.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiguang.lostfound.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
