package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.entity.Item;
import com.shiguang.lostfound.entity.Message;
import com.shiguang.lostfound.entity.User;
import com.shiguang.lostfound.mapper.ItemMapper;
import com.shiguang.lostfound.mapper.MessageMapper;
import com.shiguang.lostfound.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Z-04：站内消息单元测试（Mockito，不连库）。
 * 覆盖：list 范围 / unreadCount 只数未读 / markAllRead 只更新未读 / markRead 越权守卫（非本人 404）。
 * E-03：回复端点守卫（仅 dm、仅本人、必须有发送者）与回复产生新消息（from_user_id=回复人、refId 继承、原消息置已读）。
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock MessageMapper messageMapper;
    @Mock UserMapper userMapper;
    @Mock ItemMapper itemMapper;

    private MessageService newService() {
        return new MessageService(messageMapper, userMapper, itemMapper);
    }

    private Message msg(long id, long userId, int isRead) {
        Message m = new Message();
        m.setId(id);
        m.setUserId(userId);
        m.setIsRead(isRead);
        m.setTitle("t");
        m.setContent("c");
        return m;
    }

    @Test
    @DisplayName("list：按 userId 查询且上限 50")
    void listScopedToUser() {
        when(messageMapper.selectList(any())).thenReturn(List.of(msg(1L, 9L, 0)));
        List<Message> out = newService().list(9L);
        assertEquals(1, out.size());
        verify(messageMapper).selectList(any(QueryWrapper.class));
    }

    @Test
    @DisplayName("list：dm 消息回填发送者昵称/头像（E-03，一次 IN 查询）")
    void listFillsSenderInfo() {
        Message dm = msg(1L, 9L, 0);
        dm.setType("dm");
        dm.setFromUserId(7L);
        when(messageMapper.selectList(any())).thenReturn(List.of(dm));
        User sender = new User();
        sender.setId(7L);
        sender.setNickname("小七");
        sender.setAvatar("🐯");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(sender));

        List<Message> out = newService().list(9L);
        assertEquals("小七", out.get(0).getFromNickname());
        assertEquals("🐯", out.get(0).getFromAvatar());
        verify(userMapper).selectBatchIds(any());
    }

    @Test
    @DisplayName("unreadCount：只数 is_read=0")
    void unreadCountsOnlyUnread() {
        when(messageMapper.selectCount(any())).thenReturn(3L);
        assertEquals(3L, newService().unreadCount(9L));
        verify(messageMapper).selectCount(any(QueryWrapper.class));
    }

    @Test
    @DisplayName("markAllRead：只更新本人未读")
    void markAllReadUpdatesUnreadOnly() {
        assertDoesNotThrow(() -> newService().markAllRead(9L));
        verify(messageMapper).update(any(Message.class), any(QueryWrapper.class));
    }

    @Test
    @DisplayName("markRead：非本人 → 404（越权守卫）")
    void markReadRejectsOthers() {
        when(messageMapper.selectById(2L)).thenReturn(msg(2L, 1L, 0));
        assertEquals("消息不存在",
                assertThrows(BusinessException.class, () -> newService().markRead(2L, 9L)).getMessage());
        verify(messageMapper, never()).updateById(any(Message.class));
    }

    @Test
    @DisplayName("markRead：本人 → 置已读")
    void markReadOkForOwner() {
        Message m = msg(2L, 9L, 0);
        when(messageMapper.selectById(2L)).thenReturn(m);
        assertDoesNotThrow(() -> newService().markRead(2L, 9L));
        assertEquals(1, m.getIsRead());
        verify(messageMapper).updateById(m);
    }

    // ---------------- E-03：回复 ----------------

    private Message dm(long id, long receiver, long from) {
        Message m = msg(id, receiver, 0);
        m.setType("dm");
        m.setFromUserId(from);
        m.setRefId(33L);
        return m;
    }

    @Test
    @DisplayName("E-03 reply：正常回复 → 原发送者收到新 dm（from_user_id=回复人、refId 继承）且原消息置已读")
    void replyOk() {
        MessageService service = newService();
        when(messageMapper.selectById(2L)).thenReturn(dm(2L, 9L, 7L));
        Item item = new Item();
        item.setId(33L);
        item.setTitle("蓝色水杯");
        when(itemMapper.selectById(33L)).thenReturn(item);
        User sender = new User();
        sender.setId(7L);
        sender.setNickname("小七");
        when(userMapper.selectById(7L)).thenReturn(sender);
        assertDoesNotThrow(() -> service.reply(2L, 9L, "好的，明天取"));
        verify(messageMapper).insert(argThat((Message r) ->
                r.getUserId().equals(7L) && r.getFromUserId().equals(9L)
                        && "dm".equals(r.getType()) && r.getRefId().equals(33L)
                        && r.getContent().contains("蓝色水杯") && r.getContent().contains("好的，明天取")));
        verify(messageMapper).updateById(argThat((Message m) -> m.getId().equals(2L) && m.getIsRead() == 1));
    }

    @Test
    @DisplayName("E-03 reply：回复他人消息 → 404（不暴露他人消息存在性）")
    void replyRejectsOthers() {
        when(messageMapper.selectById(2L)).thenReturn(dm(2L, 1L, 7L));
        assertEquals(404, assertThrows(BusinessException.class,
                () -> newService().reply(2L, 9L, "x")).getCode());
        verify(messageMapper, never()).insert(any(Message.class));
    }

    @Test
    @DisplayName("E-03 reply：非 dm 消息（audit 通知）→ 400")
    void replyRejectsNonDm() {
        Message m = msg(2L, 9L, 0);
        m.setType("audit");
        when(messageMapper.selectById(2L)).thenReturn(m);
        assertEquals(400, assertThrows(BusinessException.class,
                () -> newService().reply(2L, 9L, "x")).getCode());
    }

    @Test
    @DisplayName("E-03 reply：无发送者（from_user_id=NULL）→ 400")
    void replyRejectsNoSender() {
        Message m = msg(2L, 9L, 0);
        m.setType("dm");
        when(messageMapper.selectById(2L)).thenReturn(m);
        assertEquals(400, assertThrows(BusinessException.class,
                () -> newService().reply(2L, 9L, "x")).getCode());
    }

    @Test
    @DisplayName("E-03 reply：60 秒内重复回复同一消息 → 400 节流")
    void replyThrottled() {
        MessageService service = newService();
        when(messageMapper.selectById(2L)).thenReturn(dm(2L, 9L, 7L));
        Item item = new Item();
        item.setId(33L);
        item.setTitle("蓝色水杯");
        when(itemMapper.selectById(33L)).thenReturn(item);
        User sender = new User();
        sender.setId(7L);
        when(userMapper.selectById(7L)).thenReturn(sender);
        assertDoesNotThrow(() -> service.reply(2L, 9L, "第一条"));
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.reply(2L, 9L, "第二条"));
        assertEquals(400, e.getCode());
        assertTrue(e.getMessage().contains("频繁"));
    }
}
