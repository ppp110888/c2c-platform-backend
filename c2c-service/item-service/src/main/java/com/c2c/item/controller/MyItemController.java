package com.c2c.item.controller;

import com.c2c.common.result.Result;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/item/my")
public class MyItemController {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getMyPublishedItems(@RequestHeader("X-User-Id") Long userId) {
        String sql = "SELECT * FROM item WHERE seller_id = ? ORDER BY create_time DESC";
        List<Map<String, Object>> items = jdbcTemplate.queryForList(sql, userId);
        return Result.success(items);
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> deleteMyItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long itemId) {
        try {
            String checkSql = "SELECT seller_id FROM item WHERE id = ?";
            Long sellerId = jdbcTemplate.queryForObject(checkSql, Long.class, itemId);
            if (!userId.equals(sellerId)) {
                return Result.failed(403, "无权删除他人的商品！");
            }
            String sql = "DELETE FROM item WHERE id = ?";
            jdbcTemplate.update(sql, itemId);
            return Result.success("商品已删除");
        } catch (Exception e) {
            return Result.failed(500, "删除失败：" + e.getMessage());
        }
    }

    @PostMapping("/update")
    public Result<String> updateMyItem(@RequestHeader("X-User-Id") Long userId, @RequestBody Map<String, Object> params) {
        try {
            Long itemId = Long.valueOf(params.get("id").toString());

            // 安全校验：只能修改自己的商品
            String checkSql = "SELECT seller_id FROM item WHERE id = ?";
            Long sellerId = jdbcTemplate.queryForObject(checkSql, Long.class, itemId);
            if (!userId.equals(sellerId)) {
                return Result.failed(403, "无权修改他人的商品！");
            }

            // 🚨 核心修复区：安全解析数字，防止空字符串 "" 搞崩 MySQL
            Object originalPriceObj = params.get("originalPrice");
            Double originalPrice = (originalPriceObj != null && !originalPriceObj.toString().trim().isEmpty())
                    ? Double.valueOf(originalPriceObj.toString()) : null;

            Object conditionLevelObj = params.get("conditionLevel");
            Integer conditionLevel = (conditionLevelObj != null && !conditionLevelObj.toString().trim().isEmpty())
                    ? Integer.valueOf(conditionLevelObj.toString()) : null;

            // 价格是前端必填校验过的，直接转
            Double price = Double.valueOf(params.get("price").toString());

            // 执行安全更新
            String sql = "UPDATE item SET title = ?, content = ?, price = ?, original_price = ?, condition_level = ?, city = ?, images = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                    params.get("title"),
                    params.get("content"),
                    price,
                    originalPrice,
                    conditionLevel,
                    params.get("city"),
                    params.get("images"),
                    itemId
            );

            return Result.success("商品修改成功！");

        } catch (Exception e) {
            // 🚨 架构师兜底：加上 try-catch，报错也能优雅返回给前端
            e.printStackTrace();
            return Result.failed(500, "保存失败，服务器异常：" + e.getMessage());
        }
    }
}