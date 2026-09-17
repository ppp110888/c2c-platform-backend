package com.c2c.item.controller;

import com.c2c.common.result.Result;
import com.c2c.item.service.ItemService;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/item/public")
public class PublicItemController {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private ItemService itemService; // 注入你的商品服务

    // 原有的：获取用户发布的商品列表
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getUserItems(@RequestParam("userId") Long userId) {
        String sql = "SELECT * FROM item WHERE seller_id = ? ORDER BY create_time DESC";
        List<Map<String, Object>> items = jdbcTemplate.queryForList(sql, userId);
        return Result.success(items);
    }

    // 🚨 新增：超级后门接口 —— 一键全量重建 ES 数据！
    @GetMapping("/sync-es")
    public Result<String> forceSyncEs() {
        itemService.syncItemsToEs(); // 调用你写好的全量同步方法
        return Result.success("ES 索引 item_v2 重建并同步成功！");
    }
}