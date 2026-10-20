package com.shiguang.lostfound.service;

import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.entity.Item;
import com.shiguang.lostfound.entity.ItemImage;
import com.shiguang.lostfound.entity.Message;
import com.shiguang.lostfound.entity.WatchSubscription;
import com.shiguang.lostfound.entity.WatchTarget;
import com.shiguang.lostfound.mapper.ItemImageMapper;
import com.shiguang.lostfound.mapper.ItemMapper;
import com.shiguang.lostfound.mapper.MessageMapper;
import com.shiguang.lostfound.mapper.WatchSubscriptionMapper;
import com.shiguang.lostfound.mapper.WatchTargetMapper;
import com.shiguang.lostfound.util.PHashUtil;
import com.shiguang.lostfound.vo.WatchOverviewVO;
import com.shiguang.lostfound.vo.WatchPlanVO;
import com.shiguang.lostfound.vo.WatchSubscriptionVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * v2.20 守望订阅单测（Mockito，不连库）。
 * 覆盖：方案目录数值 / 免费档自动补建 / 开通与降档配额收敛 / 守望位满额拦截 /
 * 审核通过反查命中与阈值拦截 / 结束守望的归属校验。
 */
@ExtendWith(MockitoExtension.class)
class WatchServiceTest {

    @Mock WatchSubscriptionMapper subscriptionMapper;
    @Mock WatchTargetMapper targetMapper;
    @Mock ItemMapper itemMapper;
    @Mock ItemImageMapper itemImageMapper;
    @Mock MessageMapper messageMapper;
    @Mock FileStorageService fileStorage;
    @Mock PHashUtil pHashUtil;

    private WatchService service() {
        return new WatchService(subscriptionMapper, targetMapper, itemMapper,
                itemImageMapper, messageMapper, fileStorage, pHashUtil);
    }

    private WatchSubscription sub(String plan) {
        WatchSubscription s = new WatchSubscription();
        s.setId(1L);
        s.setUserId(7L);
        s.setPlanCode(plan);
        s.setCycle("month");
        s.setStatus(1);
        s.setStartTime(LocalDateTime.now().minusDays(1));
        return s;
    }

    private WatchTarget target(long id, long phash) {
        WatchTarget t = new WatchTarget();
        t.setId(id);
        t.setUserId(7L);
        t.setPhash(phash);
        t.setLabel("黑色雨伞");
        t.setStatus(1);
        t.setHitCount(0);
        t.setCreateTime(LocalDateTime.now().minusHours(id));
        return t;
    }

    private Item foundItem() {
        Item it = new Item();
        it.setId(100L);
        it.setUserId(9L);
        it.setType("found");
        it.setTitle("食堂捡到一把黑伞");
        return it;
    }

    private ItemImage img(long phash) {
        ItemImage i = new ItemImage();
        i.setItemId(100L);
        i.setPhash(phash);
        return i;
    }

    @Test
    @DisplayName("plans：三档方案，数值与文档一致，长守为热门档")
    void plansCatalog() {
        List<WatchPlanVO> plans = service().plans();
        assertEquals(3, plans.size());
        assertEquals(List.of("light", "steady", "crowd"),
                plans.stream().map(WatchPlanVO::getCode).toList());
        assertEquals(1, plans.get(0).getQuota());
        assertEquals(6, plans.get(1).getPrice());
        assertEquals(20, plans.get(1).getTermPrice());
        assertEquals(5, plans.get(1).getQuota());
        assertEquals(12, plans.get(1).getThreshold());
        assertTrue(plans.get(1).isPopular());
        assertEquals(20, plans.get(2).getQuota());
        assertEquals(90, plans.get(2).getRetentionDays());
    }

    @Test
    @DisplayName("overview：没有订阅记录时补建免费轻守，用量为 0")
    void overviewCreatesFreePlan() {
        when(subscriptionMapper.selectOne(any())).thenReturn(null);
        when(targetMapper.selectList(any())).thenReturn(List.of());

        WatchOverviewVO vo = service().overview(7L);

        assertEquals("light", vo.getPlan().getPlanCode());
        assertEquals(1, vo.getQuota());
        assertEquals(0, vo.getUsed());
        assertTrue(vo.getTargets().isEmpty());
        verify(subscriptionMapper).insert(any(WatchSubscription.class));
    }

    @Test
    @DisplayName("subscribe：开通长守按月 → 到期时间为 30 天后")
    void subscribeSetsExpiry() {
        when(subscriptionMapper.selectOne(any())).thenReturn(sub("light"));
        when(targetMapper.selectList(any())).thenReturn(List.of());

        WatchSubscriptionVO vo = service().subscribe(7L, "steady", "month");

        assertEquals("steady", vo.getPlanCode());
        assertNotNull(vo.getExpireTime());
        LocalDateTime expire = LocalDateTime.parse(vo.getExpireTime().replace(' ', 'T'));
        assertTrue(expire.isAfter(LocalDateTime.now().plusDays(29)));
        assertTrue(expire.isBefore(LocalDateTime.now().plusDays(31)));
        // 显式 UpdateWrapper 落库（updateById 会跳过 null，降档时清不掉到期时间）
        verify(subscriptionMapper).update(isNull(), any());
    }

    @Test
    @DisplayName("subscribe：降档回轻守 → 到期时间清空")
    void subscribeDowngradeClearsExpiry() {
        WatchSubscription paid = sub("steady");
        paid.setExpireTime(LocalDateTime.now().plusDays(20));
        when(subscriptionMapper.selectOne(any())).thenReturn(paid);
        when(targetMapper.selectList(any())).thenReturn(List.of());

        WatchSubscriptionVO vo = service().subscribe(7L, "light", "month");

        assertEquals("light", vo.getPlanCode());
        assertNull(vo.getExpireTime());
        verify(subscriptionMapper).update(isNull(), any());
    }

    @Test
    @DisplayName("subscribe：降档到轻守时结束超额守望位（只留最近 1 个）")
    void subscribeDowngradeEnforcesQuota() {
        when(subscriptionMapper.selectOne(any())).thenReturn(sub("crowd"));
        when(targetMapper.selectList(any())).thenReturn(List.of(
                target(3, 3L), target(2, 2L), target(1, 1L)));

        service().subscribe(7L, "light", "month");

        ArgumentCaptor<WatchTarget> cap = ArgumentCaptor.forClass(WatchTarget.class);
        verify(targetMapper, times(2)).updateById(cap.capture());
        assertTrue(cap.getAllValues().stream().allMatch(t -> t.getStatus() == 0));
    }

    @Test
    @DisplayName("addTarget：守望位已满 → 400，且不落盘")
    void addTargetRejectsWhenFull() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("a.jpg");
        when(subscriptionMapper.selectOne(any())).thenReturn(sub("light"));
        when(targetMapper.selectCount(any())).thenReturn(1L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service().addTarget(7L, file, "雨伞", null));
        assertEquals(400, e.getCode());
        verify(fileStorage, never()).store(any(byte[].class), any());
    }

    @Test
    @DisplayName("addTarget：正常开启守望，写入 phash 与缩略图")
    void addTargetStoresHash() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("a.jpg");
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(pHashUtil.hash(any(byte[].class))).thenReturn(42L);
        when(fileStorage.store(any(byte[].class), any())).thenReturn("watch/2026/10/a.jpg");
        when(subscriptionMapper.selectOne(any())).thenReturn(sub("steady"));
        when(targetMapper.selectCount(any())).thenReturn(0L);

        service().addTarget(7L, file, "雨伞", 100L);

        ArgumentCaptor<WatchTarget> cap = ArgumentCaptor.forClass(WatchTarget.class);
        verify(targetMapper).insert(cap.capture());
        assertEquals(42L, cap.getValue().getPhash());
        assertEquals("watch/2026/10/a.jpg", cap.getValue().getImgUrl());
        assertEquals(1, cap.getValue().getStatus());
    }

    @Test
    @DisplayName("matchOnApprove：阈值内命中 → 写 type=watch 通知并累计命中")
    void matchOnApproveNotifies() {
        when(itemImageMapper.selectList(any())).thenReturn(List.of(img(0L)));
        when(targetMapper.selectList(any())).thenReturn(List.of(target(1, 0L)));
        when(subscriptionMapper.selectOne(any())).thenReturn(sub("steady"));

        int hits = service().matchOnApprove(foundItem());

        assertEquals(1, hits);
        ArgumentCaptor<Message> cap = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(cap.capture());
        assertEquals("watch", cap.getValue().getType());
        assertEquals(100L, cap.getValue().getRefId());
        assertEquals(7L, cap.getValue().getUserId());
        assertTrue(cap.getValue().getTitle().contains("100%"));
        verify(targetMapper).updateById(any(WatchTarget.class));
    }

    @Test
    @DisplayName("matchOnApprove：超出阈值 → 不打扰")
    void matchOnApproveSkipsFarHash() {
        when(itemImageMapper.selectList(any())).thenReturn(List.of(img(0L)));
        // 0b1111_1111... 前 16 位全 1，汉明距离 16 > 轻守阈值 10
        when(targetMapper.selectList(any())).thenReturn(List.of(target(1, 0xFFFFL)));
        when(subscriptionMapper.selectOne(any())).thenReturn(sub("light"));

        assertEquals(0, service().matchOnApprove(foundItem()));
        verify(messageMapper, never()).insert(any(Message.class));
    }

    @Test
    @DisplayName("matchOnApprove：寻物启事不参与反查（守望等的是招领）")
    void matchOnApproveIgnoresLost() {
        Item lost = foundItem();
        lost.setType("lost");
        assertEquals(0, service().matchOnApprove(lost));
        verify(itemImageMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("removeTarget：不是本人的守望位 → 404")
    void removeTargetRejectsForeign() {
        WatchTarget t = target(1, 1L);
        t.setUserId(8L);
        when(targetMapper.selectById(1L)).thenReturn(t);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service().removeTarget(7L, 1L));
        assertEquals(404, e.getCode());
        verify(targetMapper, never()).updateById(any(WatchTarget.class));
    }
}
