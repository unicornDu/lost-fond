package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiguang.lostfound.entity.Message;
import com.shiguang.lostfound.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 站内消息（M5 配套）：通知列表、未读数、标记已读。
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;

    public List<Message> list(Long userId) {
        return messageMapper.selectList(new QueryWrapper<Message>()
                .eq("user_id", userId).orderByDesc("create_time").last("LIMIT 50"));
    }

    public long unreadCount(Long userId) {
        return messageMapper.selectCount(new QueryWrapper<Message>()
                .eq("user_id", userId).eq("is_read", 0));
    }

    public void markAllRead(Long userId) {
        Message m = new Message();
        m.setIsRead(1);
        messageMapper.update(m, new QueryWrapper<Message>().eq("user_id", userId).eq("is_read", 0));
    }

    /** 审查 2.7 补：单条已读。仅本人可操作。 */
    public void markRead(Long messageId, Long userId) {
        Message m = messageMapper.selectById(messageId);
        if (m == null || !m.getUserId().equals(userId)) throw new com.shiguang.lostfound.common.BusinessException(404, "消息不存在");
        m.setIsRead(1);
        messageMapper.updateById(m);
    }
}
