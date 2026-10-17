package com.shiguang.lostfound.config;

import com.shiguang.lostfound.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置：
 * 1) 把上传目录映射为 /uploads/** 静态资源，前端可直接用相对路径显示图片；
 * 2) 注册 JWT 拦截器解析登录态（存在即解析，具体鉴权在各业务处校验）；
 * 3) P3 发行：后端托管已构建前端（/** → frontend/dist），浏览器直接开服务器地址即完整网站
 *    （内网穿透单条隧道的配套——API 与页面同源，Web/iOS PWA 零 CORS；控制器优先级高于静态资源，
 *    /api、/uploads 不受影响）。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileProperties fileProperties;
    private final JwtInterceptor jwtInterceptor;

    @Value("${app.frontend-dist-dir:../frontend/dist}")
    private String frontendDistDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String abs = Paths.get(fileProperties.getUploadDir()).toAbsolutePath().normalize().toString();
        String location = "file:" + abs + (abs.endsWith("\\") || abs.endsWith("/") ? "" : "/");
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);

        // P3：托管前端构建产物（dist 不存在时注册无害——访问只会 404，不影响 API）
        String dist = Paths.get(frontendDistDir).toAbsolutePath().normalize().toString();
        registry.addResourceHandler("/**")
                .addResourceLocations("file:" + dist + (dist.endsWith("\\") || dist.endsWith("/") ? "" : "/"))
                .setCachePeriod(300);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                // /api/file/apk 为公开分发端点（官网/扫码安装入口，无需登录）
                .excludePathPatterns("/api/auth/login", "/api/auth/register", "/api/file/apk");
    }
}
