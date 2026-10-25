package com.example.security;/*
 * Copyright (c) 2023. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.entity.User;
import com.example.service.UserService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

/**
 * @Description
 * @Author
 * @Data 2023/1/9 20:31
 */
@Component
public class UserDetailServiceImpl implements UserDetailsService {
    @Resource
    private UserService userService;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user=userService.getByUsername(username);
        if(user==null){
            throw new UsernameNotFoundException("用户名或者密码不正确");
        }

        return new AccountUser(user.getId(),user.getUsername(),user.getPassword(),null);
    }
    public List<GrantedAuthority> getUserAuthority(Long userId){

        // 角色(ROLE_admin)、菜单操作权限 sys:user:list
        String authority = userService.getUserAuthorityInfo(userId);

        return AuthorityUtils.commaSeparatedStringToAuthorityList(authority);
    }
}
