package com.shiguang.lostfound.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileProperties {
    /** 上传文件落盘根目录（相对后端启动目录） */
    private String uploadDir = "./uploads";
}
