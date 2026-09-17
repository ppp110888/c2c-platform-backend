package com.c2c.item.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.result.Result;
import com.c2c.item.dto.ItemSearchDTO;
import com.c2c.item.entity.Item;
import com.c2c.item.feign.UserClient;
import com.c2c.item.service.ItemService;
import com.c2c.item.vo.ItemSearchVO;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate; // 🚨 引入 Redis
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set; // 🚨 引入 Set

@RestController
@RequestMapping("/item")
public class ItemController {

    @Resource
    private ItemService itemService;

    // 注入用户服务的远程调用 Feign 客户端
    @Resource
    private UserClient userClient;

    // 注入 RocketMQ 消息发射器
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    // 🚨 新增：注入 Redis 模板
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 接口 1：发布二手商品
     */
    @PostMapping("/publish")
    public Result<String> publishItem(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Item item) {

        item.setSellerId(userId);
        item.setStatus(0); // 0代表在售
        item.setStock(1);  // 初始库存1件
        item.setViewCount(0);

        // 1. 先把商品数据存入 MySQL 数据库（极其关键，确保先落库）
        itemService.save(item);

        // ==========================================
        // 🚨 架构师补丁：暴力清除首页的 Redis 旧缓存！
        // ==========================================
        try {
            // 模糊查找并删除所有跟 item 相关的缓存 Key
            Set<String> keys = stringRedisTemplate.keys("*item*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
                System.out.println("🧹 缓存清理成功，首页将强制读取最新数据库！");
            }
        } catch (Exception e) {
            System.err.println("清理缓存失败：" + e.getMessage());
        }

        // ==========================================
        // 🚀 2. 对 RocketMQ 进行异常隔离！
        // ==========================================
        try {
            // 尝试发送消息给 ES 进行搜索同步
            rocketMQTemplate.convertAndSend("item-topic:publish", item.getId());
            System.out.println("🚀 商品发布成功！已将商品ID: " + item.getId() + " 异步发送至消息队列！");
        } catch (Exception e) {
            System.err.println("⚠️ 警告：商品已成功落库，但发送给 RocketMQ 失败 (可能MQ未启动或网络超时)。错误：" + e.getMessage());
        }

        // 3. 毫秒级返回！
        return Result.success("商品发布成功，商品ID: " + item.getId());
    }

    /**
     * 接口 2：获取首页商品列表
     */
    @GetMapping("/home/list")
    public Result<List<Item>> getHomeList() {
        List<Item> list = itemService.getHomeItemList();
        return Result.success(list);
    }

    /**
     * 接口 3：获取商品详情页
     */
    @GetMapping("/detail")
    public Result<Map<String, Object>> getItemDetail(@RequestParam("itemId") Long itemId) {
        Item item = itemService.getById(itemId);
        if (item == null) {
            return Result.failed(404, "哎呀，商品已被下架或不存在");
        }

        Result<Map<String, Object>> userResult = userClient.getUserInfo(item.getSellerId());

        Map<String, Object> detailMap = new HashMap<>();
        detailMap.put("item", item);

        if (userResult != null && userResult.getCode() == 200) {
            detailMap.put("sellerInfo", userResult.getData());
        } else {
            detailMap.put("sellerInfo", null);
        }

        item.setViewCount(item.getViewCount() + 1);
        itemService.updateById(item);

        return Result.success(detailMap);
    }

    /**
     * 同步全量数据到 ES (历史手动功能，未来可废弃)
     */
    @GetMapping("/sync-es")
    public Result<String> syncToEs(@RequestHeader(value = "X-Internal-Call", required = false) String internalCall) {
        if (!"true".equals(internalCall)) return Result.failed(403, "仅允许内部调用");
        itemService.syncItemsToEs();
        return Result.success("ES全量数据同步成功！");
    }

    /**
     * ES 搜索接口
     */
    @PostMapping("/search")
    public Result<Page<ItemSearchVO>> searchItems(@RequestBody ItemSearchDTO searchDTO) {
        Page<ItemSearchVO> resultPage = itemService.search(searchDTO);
        return Result.success(resultPage);
    }

    // =====================================================================
    // 🚨 供内部微服务（如订单服务）调用的防超卖核心接口
    // =====================================================================

    @PostMapping("/deductStock")
    public Result<String> deductStock(@RequestParam("itemId") Long itemId,
                                      @RequestHeader(value = "X-Internal-Call", required = false) String internalCall) {
        if (!"true".equals(internalCall)) return Result.failed(403, "仅允许内部调用");
        return itemService.deductStock(itemId);
    }

    @PostMapping("/addStock")
    public Result<String> addStock(@RequestParam("itemId") Long itemId,
                                   @RequestHeader(value = "X-Internal-Call", required = false) String internalCall) {
        if (!"true".equals(internalCall)) return Result.failed(403, "仅允许内部调用");
        return itemService.addStock(itemId);
    }

    // =====================================================================
    // 🚨 供订单服务 Feign 调用的轻量级商品信息接口（快照用+购物车状态检查）
    // =====================================================================
    @GetMapping("/info")
    public Result<Map<String, Object>> getItemInfo(@RequestParam("itemId") Long itemId) {
        Item item = itemService.getById(itemId);
        if (item == null) return Result.failed(404, "商品不存在");
        Map<String, Object> info = new HashMap<>();
        info.put("id", item.getId());
        info.put("title", item.getTitle());
        info.put("image", (item.getImages() != null && !item.getImages().isEmpty())
                ? item.getImages().split(",")[0] : "");
        info.put("price", item.getPrice());
        info.put("status", item.getStatus());
        info.put("sellerId", item.getSellerId());
        return Result.success(info);
    }
}
