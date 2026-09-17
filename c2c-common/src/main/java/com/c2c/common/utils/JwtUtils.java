package com.c2c.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtils {

    private static final Key SECRET_KEY = loadSecretKey();

    // 过期时间：7 天 (毫秒)
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24 * 7;

    private static Key loadSecretKey() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be set and contain at least 32 characters");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     * @param userId 用户的 ID
     * @param username 用户名
     * @return 加密后的 Token 字符串
     */
    public static String generateToken(Long userId, String username) {
        return Jwts.builder()
                .claim("userId", userId) // 存入自定义的载荷（Payload）
                .claim("username", username)
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 过期时间
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256) // 签名算法
                .compact();
    }

    /**
     * 解析并校验 JWT Token
     * @param token 客户端传来的 Token
     * @return 解析后的载荷（包含 userId 等信息）
     */
    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
