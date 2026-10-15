package com.shiguang.lostfound.service;

import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.dto.ItemForm;
import com.shiguang.lostfound.mapper.ItemImageMapper;
import com.shiguang.lostfound.mapper.ItemMapper;
import com.shiguang.lostfound.mapper.MessageMapper;
import com.shiguang.lostfound.mapper.UserMapper;
import com.shiguang.lostfound.util.PHashUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 05 A-03：发布守卫单元测试（Mockito，不连库）。
 * 覆盖：无图 400、图片超 3 张 400（B-10 上限，且超限时不产生任何磁盘写入）。
 */
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock ItemMapper itemMapper;
    @Mock ItemImageMapper itemImageMapper;
    @Mock UserMapper userMapper;
    @Mock MessageMapper messageMapper;
    @Mock FileStorageService fileStorage;
    @Mock PHashUtil pHashUtil;

    private ItemService newService() {
        return new ItemService(itemMapper, itemImageMapper, userMapper, messageMapper, fileStorage, pHashUtil);
    }

    private ItemForm form() {
        ItemForm f = new ItemForm();
        f.setType("lost");
        f.setTitle("测试标题");
        f.setCategory("其他");
        f.setLocation("图书馆");
        f.setHappenTime("2026-10-14 09:00:00");
        f.setDescription("desc");
        return f;
    }

    @Test
    @DisplayName("publish：无图被拒（审查 2.23）")
    void publishRejectsNoImage() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> newService().publish(1L, form(), new MultipartFile[0]));
        assertTrue(e.getMessage().contains("图片"));
        verify(itemMapper, never()).insert(any(com.shiguang.lostfound.entity.Item.class));
    }

    @Test
    @DisplayName("publish：超过 3 张图被拒且不落盘（B-10）")
    void publishRejectsTooManyImages() {
        MultipartFile[] files = new MultipartFile[4];
        for (int i = 0; i < 4; i++) files[i] = mock(MultipartFile.class);
        BusinessException e = assertThrows(BusinessException.class,
                () -> newService().publish(1L, form(), files));
        assertTrue(e.getMessage().contains("3 张"));
        verifyNoInteractions(fileStorage);   // 上限校验在读字节/落盘之前
    }
}
