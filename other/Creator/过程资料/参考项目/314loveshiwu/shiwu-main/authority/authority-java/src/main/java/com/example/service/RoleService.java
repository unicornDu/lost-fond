package com.example.service;

import com.example.entity.Role;
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
public interface RoleService extends IService<Role> {

    List<Role> listByUserId(long id);
}
