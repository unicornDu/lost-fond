package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.entity.Item;
import com.shiguang.lostfound.entity.Message;
import com.shiguang.lostfound.entity.User;
import com.shiguang.lostfound.mapper.ItemMapper;
import com.shiguang.lostfound.mapper.MessageMapper;
import com.shiguang.lostfound.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 站内消息（M5 配套）：通知列表、未读数、标记已读。
 * E-03：列表回填 dm 发送者昵称/头像；新增回复端点逻辑（最小双向，不建会话表——
 * 回复 = 给原发送者插一条新 dm，from_user_id=回复人，天然支持多轮）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final ItemMapper itemMapper;

    /** E-03：同一目标消息 60 秒内仅可回复 1 次（单机内存节流，与限流/浏览去重同取舍）。 */
    private static final long REPLY_THROTTLE_MS = 60 * 1000L;
    private final ConcurrentHashMap<Long, Long> replyAt = new ConcurrentHashMap<>();

    private static final int MAX_REPLY = 200;   // E-03：回复正文上限（拼前缀后仍可控制在 content 列宽内）

    public List<Message> list(Long userId) {
        List<Message> msgs = messageMapper.selectList(new QueryWrapper<Message>()
                .eq("user_id", userId).orderByDesc("create_time").last("LIMIT 50"));
        fillSenders(msgs);
        return msgs;
    }

    /** E-03：批量回填 dm 发送者昵称/头像（一次 IN 查询，避免 N+1）。 */
    private void fillSenders(List<Message> msgs) {
        Set<Long> fromIds = msgs.stream().map(Message::getFromUserId)
                .filter(id -> id != null).collect(Collectors.toSet());
        if (fromIds.isEmpty()) return;
        Map<Long, User> senders = userMapper.selectBatchIds(fromIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        for (Message m : msgs) {
            User s = senders.get(m.getFromUserId());
            if (s != null) {
                m.setFromNickname(s.getNickname());
                m.setFromAvatar(s.getAvatar());
            }
        }
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
        if (m == null || !m.getUserId().equals(userId)) throw new BusinessException(404, "消息不存在");
        m.setIsRead(1);
        messageMapper.updateById(m);
    }

    /**
     * E-03：回复一条 dm。守卫：消息存在且收件人是本人（他人 404，与 markRead 口径一致，避免探测他人消息）；
     * 仅 dm 可回复（400）；必须有 from_user_id（400）。成功后给原发送者写一条新 dm（refId 继承），
     * 并把原消息置已读。
     */
    @Transactional
    public void reply(Long messageId, Long userId, String content) {
        Message m = messageMapper.selectById(messageId);
        if (m == null || !m.getUserId().equals(userId)) throw new BusinessException(404, "消息不存在");
        if (!"dm".equals(m.getType())) throw new BusinessException(400, "仅私信可以回复");
        if (m.getFromUserId() == null) throw new BusinessException(400, "该消息没有可回复的发送者");
        if (content == null || content.isBlank()) throw new BusinessException(400, "回复内容不能为空");
        String text = content.trim();
        if (text.length() > MAX_REPLY) throw new BusinessException(400, "回复最多 " + MAX_REPLY + " 字");

        Long now = System.currentTimeMillis();
        Long last = replyAt.get(messageId);
        if (last != null && now - last < REPLY_THROTTLE_MS)
            throw new BusinessException(400, "回复太频繁，请稍后再试");
        replyAt.put(messageId, now);

        User from = userMapper.selectById(m.getFromUserId());
        if (from == null) throw new BusinessException(400, "原发送者账号已不存在，无法回复");

        String itemTitle = itemTitleOf(m.getRefId());
        String full = "回复你对「" + itemTitle + "」的私信：" + text;
        if (full.length() > 300) throw new BusinessException(400, "回复太长（含引用标题后最多 300 字），请精简后重试");
        Message reply = new Message();
        reply.setUserId(m.getFromUserId());
        reply.setFromUserId(userId);
        reply.setType("dm");
        reply.setTitle("收到新回复");
        reply.setContent(full);
        reply.setRefId(m.getRefId());
        reply.setIsRead(0);
        messageMapper.insert(reply);

        m.setIsRead(1);
        messageMapper.updateById(m);
    }

    private String itemTitleOf(Long refId) {
        if (refId == null) return "未知启事";
        Item it = itemMapper.selectById(refId);
        return it != null ? it.getTitle() : "已删除的启事";
    }
}
