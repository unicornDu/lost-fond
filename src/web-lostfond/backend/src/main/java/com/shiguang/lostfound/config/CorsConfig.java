package com.shiguang.lostfound.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * 跨域配置（修复 Z-03）：
 * - 来源外置为 web.cors.allowed-origins；为空（生产同源部署）时不放行任何跨域来源。
 * - 移除通配 OriginPattern("*") + setAllowCredentials(true)：鉴权走 Bearer 头无 Cookie，
 *   凭证放行无收益纯风险面；开发期 Vite 代理与生产 Nginx 反代本就是同源，不依赖跨域放行。
 */
@Configuration
public class CorsConfig {

    /** 允许的跨域来源；为空 = 同源部署，不注册放行（各来源必须显式列出，不再通配） */
    @Value("${web.cors.allowed-origins:}")
    private List<String> allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            config.setAllowedOrigins(allowedOrigins);
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
        }
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
