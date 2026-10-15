package com.shiguang.lostfound.service;

import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.config.FileProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 05 A-03：文件存储安全单元测试（@TempDir，不连库不连网）。
 * 覆盖 TC-M2-04：魔数白名单正例 + "改名 .jpg 的文本"反例 + 路径穿越拒绝。
 */
class FileStorageServiceTest {

    private FileStorageService newService(Path dir) {
        FileProperties props = new FileProperties();
        props.setUploadDir(dir.toString());
        return new FileStorageService(props);
    }

    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] TXT = "hello world, i am not an image".getBytes();

    @Test
    @DisplayName("validateImage：jpg/png 真魔数通过")
    void magicPositives(@TempDir Path dir) {
        FileStorageService svc = newService(dir);
        assertDoesNotThrow(() -> svc.validateImage("a.jpg", JPG));
        assertDoesNotThrow(() -> svc.validateImage("a.png", PNG));
    }

    @Test
    @DisplayName("validateImage：改名为 jpg 的文本被魔数拦截（TC-M2-04）")
    void magicRejectsRenamedText(@TempDir Path dir) {
        FileStorageService svc = newService(dir);
        assertTrue(assertThrows(BusinessException.class, () -> svc.validateImage("fake.jpg", TXT))
                .getMessage().contains("不是图片"));
    }

    @Test
    @DisplayName("validateImage：非白名单扩展名被拒")
    void rejectsBadExtension(@TempDir Path dir) {
        FileStorageService svc = newService(dir);
        assertTrue(assertThrows(BusinessException.class, () -> svc.validateImage("a.exe", JPG))
                .getMessage().contains("仅支持"));
    }

    @Test
    @DisplayName("store：合法 jpg 落盘并返回 /uploads/ 路径")
    void storeWritesFile(@TempDir Path dir) {
        FileStorageService svc = newService(dir);
        String url = svc.store(JPG, "photo.jpg");
        assertTrue(url.startsWith("/uploads/"));
        assertNotNull(svc.resolve(url));
        assertTrue(svc.resolve(url).exists());
    }

    @Test
    @DisplayName("resolve：路径穿越 ../ 被拒")
    void rejectsPathTraversal(@TempDir Path dir) {
        FileStorageService svc = newService(dir);
        assertTrue(assertThrows(BusinessException.class, () -> svc.resolve("../application-local.yml"))
                .getMessage().contains("非法文件路径"));
    }
}
