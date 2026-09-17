package com.c2c.order.controller;

import com.c2c.common.result.Result;
import com.c2c.order.entity.OrderInfo;
import com.c2c.order.feign.ItemClient;
import com.c2c.order.service.OrderService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    @Resource
    private ItemClient itemClient;

    /**
     * 提交订单接口
     * @param buyerId 从网关 Header 中透传过来的当前登录用户ID
     * @param orderInfo 前端传来的下单信息 (包含 itemId, sellerId, payAmount 等)
     */
    @PostMapping("/create")
    public Result<String> createOrder(
            @RequestHeader("X-User-Id") Long buyerId,
            @RequestBody OrderInfo orderInfo) {

        String orderNo = orderService.createOrder(buyerId, orderInfo);
        return Result.success(orderNo);  // 返回订单号，前端用于跳转支付
    }

    /**
     * 模拟内部支付接口 (备用)
     */
    @PostMapping("/pay")
    public Result<String> payOrder(
            @RequestHeader("X-User-Id") Long buyerId,
            @RequestHeader(value = "X-Internal-Call", required = false) String internalCall,
            @RequestParam("orderNo") String orderNo) {

        if (!"true".equals(internalCall)) return Result.failed(403, "仅允许内部测试调用");

        boolean success = orderService.payOrder(orderNo, buyerId);
        if (success) {
            return Result.success("支付成功！老板大气！");
        } else {
            return Result.failed(500, "支付失败，请稍后重试");
        }
    }

    /**
     * 【买家视角】我买到的订单列表
     * 🚨 架构师补丁：路径修改为 /bought，与前端请求保持绝对一致！
     */
    @GetMapping("/bought")
    public Result<List<OrderInfo>> getMyBoughtOrders(@RequestHeader("X-User-Id") Long userId) {
        List<OrderInfo> list = orderService.getMyBoughtOrders(userId);
        return Result.success(list);
    }

    /**
     * 【卖家视角】我卖出的订单列表
     * 🚨 架构师补丁：路径修改为 /sold，与前端请求保持绝对一致！
     */
    @GetMapping("/sold")
    public Result<List<OrderInfo>> getMySoldOrders(@RequestHeader("X-User-Id") Long userId) {
        List<OrderInfo> list = orderService.getMySoldOrders(userId);
        return Result.success(list);
    }

    /**
     * 订单详情
     */
    @GetMapping("/detail/{id}")
    public Result<OrderInfo> getOrderDetail(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long orderId) {
        OrderInfo order = orderService.getById(orderId);
        if (order == null) return Result.failed(404, "订单不存在");
        if (!userId.equals(order.getBuyerId()) && !userId.equals(order.getSellerId())) {
            return Result.failed(403, "无权查看该订单");
        }
        return Result.success(order);
    }

    /**
     * 取消/删除订单
     * status=0 待支付 → 取消(2) + 恢复库存
     * status=2 已取消 → 删除(3) 隐藏
     * status=1 已支付 → 拒绝
     */
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long orderId) {
        OrderInfo order = orderService.getById(orderId);
        if (order == null) return Result.failed(404, "订单不存在");
        if (!userId.equals(order.getBuyerId())) {
            return Result.failed(403, "无权操作该订单");
        }

        if (order.getStatus() == 0) {
            if (!orderService.cancelPendingOrder(order.getOrderNo())) {
                return Result.failed(409, "订单状态已变化，请刷新后重试");
            }
            itemClient.addStock(order.getItemId());
            return Result.success("订单已取消，库存已恢复");
        } else if (order.getStatus() == 2) {
            order.setStatus(3);
            orderService.updateById(order);
            return Result.success("订单已删除");
        } else {
            return Result.failed(400, "已支付订单不支持删除");
        }
    }
}
