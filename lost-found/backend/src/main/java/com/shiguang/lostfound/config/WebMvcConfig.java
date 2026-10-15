package com.shiguang.lostfound.config;

import com.shiguang.lostfound.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置：
 * 1) 把上传目录映射为 /uploads/** 静态资源，前端可直接用相对路径显示图片；
 * 2) 注册 JWT 拦截器解析登录态（存在即解析，具体鉴权在各业务处校验）。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileProperties fileProperties;
    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String abs = Paths.get(fileProperties.getUploadDir()).toAbsolutePath().normalize().toString();
        String location = "file:" + abs + (abs.endsWith("\\") || abs.endsWith("/") ? "" : "/");
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/register");
    }
}
