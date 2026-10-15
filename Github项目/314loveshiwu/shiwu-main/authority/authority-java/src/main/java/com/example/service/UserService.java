package com.example.service;

import com.example.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author   
 * @since 2023-01-07
 */
public interface UserService extends IService<User> {
    User getByUsername(String username);

    String getUserAuthorityInfo(Long userId);
    void clearUserAuthorityInfo(String username);
    void clearUserAuthorityByRole(Long roleId);
    void clearUserAuthorityByMenuId(Long menuId);



}
