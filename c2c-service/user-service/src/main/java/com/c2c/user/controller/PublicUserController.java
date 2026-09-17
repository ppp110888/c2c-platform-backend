package com.c2c.user.controller;

import com.c2c.common.result.Result;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/user/public")
public class PublicUserController {

    @Resource
    private JdbcTemplate jdbcTemplate;

    // 获取公开的用户信息（头像、昵称、介绍）
    @GetMapping("/info")
    public Result<Map<String, Object>> getPublicInfo(@RequestParam("userId") Long userId) {
        try {
            String sql = "SELECT id, nickname, avatar, bio FROM user WHERE id = ?";
            Map<String, Object> user = jdbcTemplate.queryForMap(sql, userId);
            return Result.success(user);
        } catch (Exception e) {
            return Result.failed(404, "用户不存在");
        }
    }
}