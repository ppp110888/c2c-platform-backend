package com.c2c.item.controller;

import com.c2c.common.result.Result;
import com.c2c.item.entity.Item;
import com.c2c.item.service.ItemService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/item/history")
public class HistoryController {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ItemService itemService;

    // 1. 添加浏览记录 (前端每次进详情页调用)
    @PostMapping("/add")
    public Result<String> addHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("itemId") Long itemId) {

        String key = "user:history:" + userId;
        // 用当前时间戳作为 score，保证最新的浏览记录永远排在前面，且同一个商品会自动去重更新时间
        stringRedisTemplate.opsForZSet().add(key, itemId.toString(), System.currentTimeMillis());

        // 🚨 架构师精髓：限制每个人最多只保留最近 50 条记录，防止 Redis 内存撑爆
        stringRedisTemplate.opsForZSet().removeRange(key, 0, -51);

        return Result.success("记录成功");
    }

    // 2. 获取浏览记录列表 (前端进入“我的足迹”调用)
    @GetMapping("/list")
    public Result<List<Item>> getHistoryList(@RequestHeader("X-User-Id") Long userId) {
        String key = "user:history:" + userId;

        // 按时间戳倒序获取商品 ID (最新的排前面)
        Set<String> itemIds = stringRedisTemplate.opsForZSet().reverseRange(key, 0, 49);

        if (itemIds == null || itemIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 根据缓存里的 ID 查询商品详情
        List<Item> historyItems = new ArrayList<>();
        for (String idStr : itemIds) {
            Item item = itemService.getById(Long.valueOf(idStr));
            if (item != null) {
                historyItems.add(item);
            }
        }
        return Result.success(historyItems);
    }
}