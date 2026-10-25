package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.common.PageResult;
import com.shiguang.lostfound.entity.Favorite;
import com.shiguang.lostfound.entity.Item;
import com.shiguang.lostfound.mapper.FavoriteMapper;
import com.shiguang.lostfound.mapper.ItemMapper;
import com.shiguang.lostfound.vo.ItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * E-07：启事收藏。
 * 收藏守卫与公开可见口径同源（audit=1 且非已下架/已关闭，复用 ItemService 的公开口径判断）；
 * add/remove 均幂等（唯一索引 + delete 天然幂等）；启事删除由 AdminService.deleteItem 级联清理收藏行。
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ItemMapper itemMapper;
    private final ItemService itemService;

    private static final long MAX_PAGE_SIZE = 100;

    /** 是否公开可见（与 ItemService.listPublicByUser 同一口径：audit=1 且非已下架/已关闭）。 */
    private boolean publiclyVisible(Item it) {
        return it.getAuditStatus() != null && it.getAuditStatus() == 1
                && !"已下架".equals(it.getStatus()) && !"已关闭".equals(it.getStatus());
    }

    /** 收藏（幂等：已收藏直接返回成功）。目标不可见/不存在 → 400/404。 */
    public void add(Long userId, Long itemId) {
        Item it = itemMapper.selectById(itemId);
        if (it == null) throw new BusinessException(404, "启事不存在");
        if (!publiclyVisible(it)) throw new BusinessException(400, "该启事当前不可收藏");
        Long dup = favoriteMapper.selectCount(new QueryWrapper<Favorite>()
                .eq("user_id", userId).eq("item_id", itemId));
        if (dup != null && dup > 0) return;
        Favorite f = new Favorite();
        f.setUserId(userId);
        f.setItemId(itemId);
        favoriteMapper.insert(f);
    }

    /** 取消收藏（幂等：无该行也返回成功）。 */
    public void remove(Long userId, Long itemId) {
        favoriteMapper.delete(new QueryWrapper<Favorite>()
                .eq("user_id", userId).eq("item_id", itemId));
    }

    /** 我的收藏（分页，收藏时间倒序）。卡片口径复用 ItemService.toVO；启事已删除的行防御性跳过。 */
    public PageResult<ItemVO> list(Long userId, long page, long size) {
        page = Math.max(1, page);
        size = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        Page<Favorite> p = favoriteMapper.selectPage(new Page<>(page, size),
                new QueryWrapper<Favorite>().eq("user_id", userId).orderByDesc("create_time"));
        Set<Long> itemIds = p.getRecords().stream().map(Favorite::getItemId).collect(Collectors.toSet());
        Map<Long, Item> itemMap = itemIds.isEmpty() ? Map.of()
                : itemMapper.selectBatchIds(itemIds).stream()
                    .collect(Collectors.toMap(Item::getId, i -> i, (a, b) -> a));
        Map<Long, String> nick = itemService.loadNicknames(itemMap.values().stream()
                .map(Item::getUserId).collect(Collectors.toSet()));
        List<ItemVO> records = p.getRecords().stream()
                .map(f -> itemMap.get(f.getItemId()))
                .filter(it -> it != null && publiclyVisible(it))
                .map(it -> itemService.toVO(it, nick, null))
                .collect(Collectors.toList());
        return PageResult.of(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 启事删除时级联清理（对齐 B-05/B-11 的级联清理风格，由 AdminService.deleteItem 调用）。 */
    public void deleteByItem(Long itemId) {
        favoriteMapper.delete(new QueryWrapper<Favorite>().eq("item_id", itemId));
    }
}
