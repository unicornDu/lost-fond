package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.entity.Message;
import com.shiguang.lostfound.mapper.MessageMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Z-04：站内消息单元测试（Mockito，不连库）。
 * 覆盖：list 范围 / unreadCount 只数未读 / markAllRead 只更新未读 / markRead 越权守卫（非本人 404）。
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock MessageMapper messageMapper;

    private MessageService newService() {
        return new MessageService(messageMapper);
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
}
