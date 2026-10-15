package com.example.service.Impl;

import com.example.entity.Help;
import com.example.mapper.HelpMapper;
import com.example.service.HelpService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 失物求助管理 服务实现类
 * </p>
 *
 * @author ren
 * @since 2023-02-13
 */
@Service
public class HelpServiceImpl extends ServiceImpl<HelpMapper, Help> implements HelpService {

}
