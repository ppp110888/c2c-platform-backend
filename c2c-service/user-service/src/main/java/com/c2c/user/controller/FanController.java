package com.c2c.user.controller;

import com.c2c.common.result.Result;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user/fans")
public class FanController {

    @Resource
    private JdbcTemplate jdbcTemplate;

    // 获取我的粉丝列表 (即 target_id 是我的那些关注记录)
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getFansList(@RequestHeader("X-User-Id") Long userId) {
        // 联表查询：找出谁关注了我，并查出他们的头像、昵称和签名
        String sql = "SELECT u.id, u.nickname, u.avatar, u.bio " +
                "FROM user u " +
                "JOIN user_follow f ON u.id = f.user_id " +
                "WHERE f.target_id = ? " +
                "ORDER BY f.create_time DESC";

        List<Map<String, Object>> fans = jdbcTemplate.queryForList(sql, userId);
        return Result.success(fans);
    }
}