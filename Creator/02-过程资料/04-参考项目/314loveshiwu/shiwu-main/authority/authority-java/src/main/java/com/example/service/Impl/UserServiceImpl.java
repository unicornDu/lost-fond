package com.example.service.Impl;

import cn.hutool.json.JSONUtil;
import com.example.entity.Menu;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.mapper.UserMapper;
import com.example.service.MenuService;
import com.example.service.RoleService;
import com.example.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.utils.RedisUtil;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author   
 * @since 2023-01-07
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Resource
    RoleService sysRoleService;
    @Resource
    UserMapper sysUserMapper;
    @Resource
    MenuService sysMenuService;
    @Resource
    RedisUtil redisUtil;
    @Override
    public User getByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return getOne(queryWrapper);
    }

    @Override
    public String getUserAuthorityInfo(Long userId) {
//        用户的角色：ROLE_ADMIN，ROLE_USER,ROLE_TEST
//        获取角色

//        String authority = "";
//        //获取用户权限缓存
//        User sysUser = getById(userId);
//
//        if (redisUtil.hasKey("GrantedAuthority:" + sysUser.getUsername())) {
//            authority = (String) redisUtil.get("GrantedAuthority:" + sysUser.getUsername());
//
//        } else {
//
//            QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
//            queryWrapper.inSql("id", "select role_id from sys_user_role where user_id= " + userId);
//            List<Role> roles = sysRoleService.list(queryWrapper);
//
//            if (roles.size() > 0) {
//                String rolecodes = roles.stream().map(m -> "ROLE_" + m.getCode()).collect(Collectors.joining(","));
//                authority = rolecodes.concat(",");
//            }
//
////        获取菜单操作权限编码code
//            List<Long> menuIds = sysUserMapper.getNavMenuIds(userId);
//
//
//
//            if (menuIds.size() > 0) {
//                List<Menu> sysMenus = sysMenuService.listByIds(menuIds);
//
//                String menusPerms = sysMenus.stream().map(m -> m.getPerms()).collect(Collectors.joining(","));
//                authority = authority.concat(menusPerms);
//
//            }
//
//        }
//        redisUtil.set("GrantedAuthority:" + sysUser.getUsername(), authority, 60 * 60);
//        return authority;
        String authority = "";
        //获取用户权限缓存
       User sysUser = getById(userId);
//        if (redisUtil.hasKey("GrantedAuthority:" + sysUser.getUsername())) {
//            authority = (String) redisUtil.get("GrantedAuthority:" + sysUser.getUsername());
//
//        } else {

            QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
            queryWrapper.inSql("id", "select role_id from sys_user_role where user_id= " + userId);
            List<Role> roles = sysRoleService.list(queryWrapper);
            if (roles.size() > 0) {
                String rolecodes = roles.stream().map(m -> "ROLE_" + m.getCode()).collect(Collectors.joining(","));
                authority = rolecodes.concat(",");
            }

//        获取菜单操作权限编码code
            List<Long> menuIds = sysUserMapper.getNavMenuIds(userId);


            if (menuIds.size() > 0) {
                List<Menu> sysMenus= sysMenuService.listByIds(menuIds);

                String menusPerms = sysMenus.stream().map(m -> m.getPerms()).collect(Collectors.joining(","));
                authority = authority.concat(menusPerms);

            }

//        }

        redisUtil.set("GrantedAuthority:" + sysUser.getUsername(), authority, 60 * 60);
        return authority;

    }

    @Override
    public void clearUserAuthorityInfo(String username) {
        redisUtil.del("GrantedAuthority:"+username);
    }

    @Override
    public void clearUserAuthorityByRole(Long roleId) {
        List<User> sysUsers = this.list(new QueryWrapper<User>()
                .inSql("id", "select user_id from sys_user_role where role_id = " + roleId));

        sysUsers.forEach(u -> {
            this.clearUserAuthorityInfo(u.getUsername());
        });

    }

    @Override
    public void clearUserAuthorityByMenuId(Long menuId) {
        List<User> sysUsers = sysUserMapper.listByMenuId(menuId);

        sysUsers.forEach(u -> {
            this.clearUserAuthorityInfo(u.getUsername());
        });
    }
}
