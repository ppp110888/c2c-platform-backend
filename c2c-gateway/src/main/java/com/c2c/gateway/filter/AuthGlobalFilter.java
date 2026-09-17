package com.c2c.gateway.filter;

import cn.hutool.json.JSONUtil;
import com.c2c.common.result.Result;
import com.c2c.common.result.ResultCode;
import com.c2c.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Value("${security.internal-secret}")
    private String internalSecret;

    // 路径匹配器
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    // 白名单路径：不需要登录就能访问的接口
    private final List<String> whiteList = Arrays.asList(
            "/api/user/login",
            "/api/user/register",
            "/api/item/home/list",
            "/api/item/detail",
            "/api/item/search",         // 允许游客搜索
            "/api/order/alipay/pay",    // 收银台由随机订单号定位，支付结果仍需支付宝验签
            "/api/order/alipay/return", // 支付宝同步回调（支付成功后跳回）
            "/api/order/alipay/notify", // 支付宝异步回调通知
            "/api/user/public/**",
            "/api/item/public/**",
            "/api/order/review/list"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 判断是否在白名单中，如果是，直接放行
        for (String whitePath : whiteList) {
            if (antPathMatcher.match(whitePath, path)) {
                ServerHttpRequest publicRequest = trustedRequest(request, null);
                return chain.filter(exchange.mutate().request(publicRequest).build());
            }
        }

        // 2. 从 HTTP 请求头中获取 Token
        String token = request.getHeaders().getFirst("Authorization");

        // 3. 校验 Token 是否存在
        if (!StringUtils.hasText(token)) {
            log.warn("=== 请求被拦截，未携带Token: {} ===", path);
            return unauthorizedResponse(exchange);
        }

        // 4. 解析并校验 Token
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Claims claims = JwtUtils.parseToken(token);
            Long userId = claims.get("userId", Long.class);

            ServerHttpRequest mutatedRequest = trustedRequest(request, userId);

            exchange = exchange.mutate().request(mutatedRequest).build();

        } catch (Exception e) {
            log.warn("=== 请求被拦截，Token无效或已过期: {} ===", path);
            return unauthorizedResponse(exchange);
        }

        // 5. 校验通过，放行请求
        return chain.filter(exchange);
    }

    private ServerHttpRequest trustedRequest(ServerHttpRequest request, Long userId) {
        ServerHttpRequest.Builder builder = request.mutate();
        builder.headers(headers -> {
            headers.remove("X-User-Id");
            headers.remove("X-Internal-Secret");
            headers.remove("X-Internal-Call");
        });
        builder.header("X-Internal-Secret", internalSecret);
        if (userId != null) {
            builder.header("X-User-Id", String.valueOf(userId));
        }
        return builder.build();
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<?> result = Result.failed(ResultCode.UNAUTHORIZED);
        byte[] bytes = JSONUtil.toJsonStr(result).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
