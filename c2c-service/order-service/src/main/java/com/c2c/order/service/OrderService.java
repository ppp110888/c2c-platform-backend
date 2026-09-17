package com.c2c.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.c2c.order.entity.OrderInfo;

import java.util.List;

public interface OrderService extends IService<OrderInfo> {
    // 创建订单
    String createOrder(Long buyerId, OrderInfo orderInfo);

    // 🚨 新增：模拟支付订单
    boolean payOrder(String orderNo, Long buyerId);

    // 🚨 新增 1：买家视角 - 查询我买到的订单
    List<OrderInfo> getMyBoughtOrders(Long buyerId);

    // 🚨 新增 2：卖家视角 - 查询我卖出的订单
    List<OrderInfo> getMySoldOrders(Long sellerId);

    // 新增：供支付宝回调使用的支付成功处理逻辑 (不需要 buyerId)
    boolean payOrderByCallback(String orderNo);

    boolean cancelPendingOrder(String orderNo);

}
