package com.example.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.controller.dto.MenuDto;
import com.example.entity.Menu;
import com.example.entity.User;
import com.example.mapper.MenuMapper;
import com.example.mapper.UserMapper;
import com.example.service.MenuService;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author
 * @since 2021-04-05
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    @Autowired
    UserService sysUserService;

    @Autowired
    UserMapper sysUserMapper;

    @Override
    public List<MenuDto> getCurrentUserNavs() {
//		获取账户名
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//		 获取用户信息
        User user = sysUserService.getByUsername(username);
//		获取用户的菜单id表
        List<Long> menuIds = sysUserMapper.getNavMenuIds(user.getId());

//		获取用户的菜单信息

        List<Menu> menus = this.listByIds(menuIds);
//        排序
        menus.sort(Comparator.comparing(Menu::getOrderNum));
//		转换为树状结构
        List<Menu> menuTree = buildTree(menus);

//		转换成dto
        return convert(menuTree);

    }

    /**
     * 生成树状导航栏菜单表格
     *
     * @return
     */
    @Override
    public List<Menu> getTree() {
//        获取所有菜单信息

        List<Menu> menus = this.list(new QueryWrapper<Menu>().orderByAsc("orderNum"));

//        转成树状结构
        return buildTree(menus);

    }

    private List<MenuDto> convert(List<Menu> menuTree) {
        List<MenuDto> mList = new ArrayList<>();
        menuTree.forEach(m -> {
            MenuDto menuDto = new MenuDto();
            menuDto.setId(m.getId());
            menuDto.setName(m.getPerms());
            menuDto.setTitle(m.getName());
            menuDto.setIcon(m.getIcon());
            menuDto.setComponent(m.getComponent());
            menuDto.setOrderNum(m.getOrderNum());
            menuDto.setPath(m.getPath());
//            递归调用,当子级别菜单满足条件是即可继续使用此方法重头开始执行
            if (m.getChildren().size() > 0) {
                menuDto.setChildren(convert(m.getChildren()));
            }
            mList.add(menuDto);
        });
        return mList;
    }

    //	转换树状结构
    private List<Menu> buildTree(List<Menu> menus) {

        List<Menu> finalMenus = new ArrayList<>();
//		通过父类找子类，通过id找
        for (Menu menu : menus) {
            for (Menu e : menus) {

                if (menu.getId() == e.getParentId()) {

                    menu.getChildren().add(e);
                }
            }
            if (menu.getParentId() == 0L) {
                finalMenus.add(menu);
            }
        }
//		提取出父节点


        return finalMenus;
    }


}
