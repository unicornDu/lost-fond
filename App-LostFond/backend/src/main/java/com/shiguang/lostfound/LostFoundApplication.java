package com.shiguang.lostfound;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 拾光 · 校园失物招领平台 后端启动类。
 * 技术栈：Spring Boot 3.3 + MyBatis-Plus + MySQL + JJWT + JImageHash(pHash) + Knife4j。
 */
@SpringBootApplication
@MapperScan("com.shiguang.lostfound.mapper")
public class LostFoundApplication {
    public static void main(String[] args) {
        SpringApplication.run(LostFoundApplication.class, args);
    }
}
