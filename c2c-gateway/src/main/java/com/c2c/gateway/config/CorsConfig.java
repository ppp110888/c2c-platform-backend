package com.c2c.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {
    @Bean
    public CorsWebFilter corsWebFilter(@Value("${security.allowed-origins:http://localhost:5173}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 允许所有前端域名跨域调用 (生产环境建议写死前端域名)
        for (String origin : origins.split(",")) {
            config.addAllowedOrigin(origin.trim());
        }
        // 允许所有的请求方法 (GET, POST, PUT, DELETE 等)
        config.addAllowedMethod("*");
        // 允许携带 Cookie
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有经过网关的路由都生效跨域规则
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
