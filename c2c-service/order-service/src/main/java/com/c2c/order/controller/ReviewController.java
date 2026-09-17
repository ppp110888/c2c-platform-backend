package com.c2c.order.controller;

import com.c2c.common.result.Result;
import com.c2c.order.feign.UserClient;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/order/review")
public class ReviewController {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private UserClient userClient;

    // 1. 发布评价 (买家评卖家 / 卖家评买家 均可使用)
    @PostMapping("/add")
    public Result<String> addReview(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> params) {

        try {
            Long orderId = Long.valueOf(params.get("orderId").toString());
            Long targetId = Long.valueOf(params.get("targetId").toString());
            Long itemId = Long.valueOf(params.get("itemId").toString());
            Integer rating = Integer.valueOf(params.get("rating").toString());
            String content = params.get("content").toString();

            if (rating < 1 || rating > 5 || content.length() > 500) {
                return Result.failed(400, "评分必须为1到5，评价内容不能超过500字");
            }
            Map<String, Object> order = jdbcTemplate.queryForMap(
                    "SELECT buyer_id, seller_id, item_id, status FROM order_info WHERE id = ?", orderId);
            Long buyerId = Long.valueOf(order.get("buyer_id").toString());
            Long sellerId = Long.valueOf(order.get("seller_id").toString());
            Long expectedTarget = userId.equals(buyerId) ? sellerId : buyerId;
            if ((!userId.equals(buyerId) && !userId.equals(sellerId))
                    || Integer.parseInt(order.get("status").toString()) != 1
                    || !expectedTarget.equals(targetId)
                    || !Long.valueOf(order.get("item_id").toString()).equals(itemId)) {
                return Result.failed(403, "无权评价该订单");
            }

            // 🚨 防刷校验：这笔订单，当前用户是否已经评价过？
            String checkSql = "SELECT COUNT(*) FROM order_review WHERE order_id = ? AND reviewer_id = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, orderId, userId);
            if (count != null && count > 0) {
                return Result.failed(400, "您已经评价过该订单啦，不能重复评价哦！");
            }

            // 执行插入评价数据
            String sql = "INSERT INTO order_review (order_id, reviewer_id, target_id, item_id, rating, content) VALUES (?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, orderId, userId, targetId, itemId, rating, content);

            return Result.success("评价发布成功！");
        } catch (org.springframework.dao.DuplicateKeyException e) {
            return Result.failed(409, "您已经评价过该订单");
        } catch (Exception e) {
            return Result.failed(400, "评价参数无效或订单不存在");
        }
    }

    // 2. 获取某个人收到的所有评价 (支持 enrich 用户信息)
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listReviews(
            @RequestParam("targetId") Long targetId,
            @RequestParam(value = "enrich", defaultValue = "false") boolean enrich) {
        String sql = "SELECT * FROM order_review WHERE target_id = ? ORDER BY create_time DESC";
        List<Map<String, Object>> reviews = jdbcTemplate.queryForList(sql, targetId);
        if (enrich) {
            enrichWithUserInfo(reviews, "reviewer_id");
        }
        return Result.success(reviews);
    }

    // 3. 我的评价（收到的 + 发出的）
    @GetMapping("/my")
    public Result<Map<String, Object>> getMyReviews(@RequestHeader("X-User-Id") Long userId) {
        String receivedSql = "SELECT * FROM order_review WHERE target_id = ? ORDER BY create_time DESC";
        List<Map<String, Object>> received = jdbcTemplate.queryForList(receivedSql, userId);

        String givenSql = "SELECT * FROM order_review WHERE reviewer_id = ? ORDER BY create_time DESC";
        List<Map<String, Object>> given = jdbcTemplate.queryForList(givenSql, userId);

        enrichWithUserInfo(received, "reviewer_id");
        enrichWithUserInfo(given, "target_id");

        Map<String, Object> result = new HashMap<>();
        result.put("received", received);
        result.put("given", given);
        return Result.success(result);
    }

    // ============ 私有方法：通过 Feign 补充用户头像昵称 ============
    private void enrichWithUserInfo(List<Map<String, Object>> reviews, String userIdKey) {
        Set<Long> userIds = new HashSet<>();
        for (Map<String, Object> r : reviews) {
            Object uid = r.get(userIdKey);
            if (uid != null) userIds.add(Long.valueOf(uid.toString()));
        }
        Map<Long, Map<String, Object>> userCache = new HashMap<>();
        for (Long uid : userIds) {
            try {
                Result<Map<String, Object>> userRes = userClient.getUserInfo(uid);
                if (userRes.getCode() == 200 && userRes.getData() != null) {
                    userCache.put(uid, userRes.getData());
                }
            } catch (Exception e) { /* 单个用户查询失败不影响整体 */ }
        }
        for (Map<String, Object> r : reviews) {
            Object uid = r.get(userIdKey);
            if (uid != null) {
                Map<String, Object> u = userCache.get(Long.valueOf(uid.toString()));
                if (u != null) {
                    r.put("reviewer_nickname", u.getOrDefault("nickname", "神秘用户"));
                    r.put("reviewer_avatar", u.getOrDefault("avatar", ""));
                } else {
                    r.put("reviewer_nickname", "神秘用户");
                    r.put("reviewer_avatar", "");
                }
            }
        }
    }
}
