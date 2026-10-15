package com.example.service;

import com.example.controller.dto.MenuDto;
import com.example.entity.Menu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author   
 * @since 2023-01-09
 */
public interface MenuService extends IService<Menu> {

    List<MenuDto> getCurrentUserNavs();

    List<Menu> getTree();
}
