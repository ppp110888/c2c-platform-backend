package com.c2c.order.config;

import feign.RequestInterceptor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Configuration
public class InternalSecurityConfig {
    @Bean
    FilterRegistrationBean<OncePerRequestFilter> internalRequestFilter(
            @Value("${security.internal-secret}") String expectedSecret) {
        OncePerRequestFilter filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain) throws java.io.IOException, jakarta.servlet.ServletException {
                String supplied = request.getHeader("X-Internal-Secret");
                boolean valid = supplied != null && MessageDigest.isEqual(
                        supplied.getBytes(StandardCharsets.UTF_8), expectedSecret.getBytes(StandardCharsets.UTF_8));
                if (!valid) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Direct service access is forbidden");
                    return;
                }
                chain.doFilter(request, response);
            }
        };
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
    }

    @Bean
    RequestInterceptor internalFeignInterceptor(@Value("${security.internal-secret}") String secret) {
        return template -> {
            template.header("X-Internal-Secret", secret);
            template.header("X-Internal-Call", "true");
        };
    }
}
