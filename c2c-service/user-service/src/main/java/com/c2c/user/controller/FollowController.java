package com.c2c.user.controller;

import com.c2c.common.result.Result;
import com.c2c.user.entity.User;
import com.c2c.user.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/user/follow")
public class FollowController {

    // 直接使用 Spring 底层的 JDBC 模板，极速执行 SQL
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private UserService userService;

    // 1. 关注 / 取消关注 (二合一接口)
    @PostMapping("/toggle")
    public Result<String> toggle(@RequestHeader("X-User-Id") Long userId, @RequestParam("targetId") Long targetId) {
        if (userId.equals(targetId)) return Result.failed(400, "不能关注自己哦");

        String checkSql = "SELECT COUNT(*) FROM user_follow WHERE user_id = ? AND target_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, targetId);

        if (count != null && count > 0) {
            // 已经关注过了，执行取消关注
            jdbcTemplate.update("DELETE FROM user_follow WHERE user_id = ? AND target_id = ?", userId, targetId);
            return Result.success("已取消关注");
        } else {
            // 没关注过，执行关注
            jdbcTemplate.update("INSERT INTO user_follow (user_id, target_id) VALUES (?, ?)", userId, targetId);
            return Result.success("关注成功");
        }
    }

    // 2. 检查是否已关注 (用于商品详情页点亮按钮)
    @GetMapping("/check")
    public Result<Boolean> check(@RequestHeader("X-User-Id") Long userId, @RequestParam("targetId") Long targetId) {
        String checkSql = "SELECT COUNT(*) FROM user_follow WHERE user_id = ? AND target_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, targetId);
        return Result.success(count != null && count > 0);
    }

    // 3. 获取我的关注列表
    @GetMapping("/list")
    public Result<List<User>> list(@RequestHeader("X-User-Id") Long userId) {
        String sql = "SELECT target_id FROM user_follow WHERE user_id = ?";
        List<Long> targetIds = jdbcTemplate.queryForList(sql, Long.class, userId);

        List<User> users = new ArrayList<>();
        for (Long tid : targetIds) {
            User u = userService.getById(tid);
            if (u != null) {
                u.setPassword(null); // 脱敏，防止密码泄露
                users.add(u);
            }
        }
        return Result.success(users);
    }
}