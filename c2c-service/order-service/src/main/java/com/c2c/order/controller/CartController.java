package com.c2c.order.controller;

import cn.hutool.json.JSONUtil;
import com.c2c.common.result.Result;
import com.c2c.order.feign.ItemClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/cart")
public class CartController {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ItemClient itemClient;

    // 定义 Redis 购物车的 Key 前缀
    private static final String CART_PREFIX = "c2c:cart:";

    /**
     * 1. 获取当前用户的购物车列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getCartList(@RequestHeader("X-User-Id") Long userId) {
        String cartKey = CART_PREFIX + userId;

        // 瞬间从 Redis 拉取该用户购物车里的所有商品！
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(cartKey);

        // 将 JSON 字符串转换回对象，并检查商品当前状态（标记已售出）
        List<Map<String, Object>> cartList = new ArrayList<>();
        for (Object value : entries.values()) {
            Map<String, Object> cartItem = JSONUtil.parseObj((String) value);
            Object itemIdObj = cartItem.get("itemId");
            if (itemIdObj != null) {
                try {
                    Result<Map<String, Object>> infoRes = itemClient.getItemInfo(Long.valueOf(itemIdObj.toString()));
                    if (infoRes.getCode() == 200 && infoRes.getData() != null) {
                        cartItem.put("itemStatus", infoRes.getData().get("status"));
                    } else {
                        // 商品已下架（被删除）或查询失败
                        cartItem.put("itemStatus", -1);
                    }
                } catch (Exception e) {
                    // Feign 调用异常，也视为已下架
                    cartItem.put("itemStatus", -1);
                }
            }
            cartList.add(cartItem);
        }

        return Result.success(cartList);
    }

    /**
     * 2. 加入购物车
     */
    @PostMapping("/add")
    public Result<String> addCart(@RequestHeader("X-User-Id") Long userId, @RequestBody Map<String, Object> cartItem) {
        String cartKey = CART_PREFIX + userId;
        String itemId = String.valueOf(cartItem.get("itemId"));

        // 将前端传来的商品快照（价格、标题、图片）存入 Redis Hash 中
        stringRedisTemplate.opsForHash().put(cartKey, itemId, JSONUtil.toJsonStr(cartItem));
        return Result.success("加入购物车成功");
    }

    /**
     * 3. 移出购物车
     */
    @DeleteMapping("/delete/{itemId}")
    public Result<String> deleteCartItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("itemId") String itemId) { // 🚨 核心修复：加上 ("itemId")

        String cartKey = CART_PREFIX + userId;
        stringRedisTemplate.opsForHash().delete(cartKey, itemId);
        return Result.success("删除成功");
    }
}