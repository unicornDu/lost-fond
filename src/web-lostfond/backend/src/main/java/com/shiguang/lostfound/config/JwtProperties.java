package com.shiguang.lostfound.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long expireMinutes = 1440;      // 默认 1 天
    private long rememberMinutes = 43200;   // 记住我 30 天（审查 2.40）
    private String header = "Authorization";
    private String prefix = "Bearer ";
}
