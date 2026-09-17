package com.c2c.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.c2c.common.result.Result;
import com.c2c.order.entity.OrderInfo;
import com.c2c.order.feign.ItemClient;
import com.c2c.order.feign.UserClient;
import com.c2c.order.mapper.OrderInfoMapper;
import com.c2c.order.service.OrderService;
import jakarta.annotation.Resource;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    @Resource
    private ItemClient itemClient;

    @Resource
    private UserClient userClient;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public String createOrder(Long buyerId, OrderInfo orderInfo) {

        // 1. 生成单号，先落库（先占位）
        String orderNo = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        orderInfo.setOrderNo(orderNo);
        orderInfo.setBuyerId(buyerId);
        orderInfo.setStatus(0); // 待支付

        if (orderInfo.getItemId() == null || orderInfo.getAddressId() == null) {
            throw new IllegalArgumentException("商品和收货地址不能为空");
        }

        // 🚀 快照：拉取商品信息（标题、首图、价格）
        try {
            Result<Map<String, Object>> itemResult = itemClient.getItemInfo(orderInfo.getItemId());
            if (itemResult.getCode() == 200 && itemResult.getData() != null) {
                Map<String, Object> itemData = itemResult.getData();
                if (!Integer.valueOf(0).equals(Integer.valueOf(itemData.get("status").toString()))) {
                    throw new IllegalStateException("商品已下架或售出");
                }
                orderInfo.setItemTitle((String) itemData.get("title"));
                orderInfo.setItemImage((String) itemData.get("image"));
                Object priceObj = itemData.get("price");
                if (priceObj == null || itemData.get("sellerId") == null) throw new IllegalStateException("商品数据不完整");
                BigDecimal authoritativePrice = new BigDecimal(priceObj.toString());
                Long authoritativeSellerId = Long.valueOf(itemData.get("sellerId").toString());
                if (buyerId.equals(authoritativeSellerId)) throw new IllegalArgumentException("不能购买自己的商品");
                orderInfo.setSellerId(authoritativeSellerId);
                orderInfo.setItemPrice(authoritativePrice);
                orderInfo.setPayAmount(authoritativePrice);
            } else {
                throw new IllegalArgumentException("商品不存在");
            }
        } catch (Exception e) {
            throw new IllegalStateException("无法获取有效商品信息", e);
        }

        // 🚀 快照：拉取收货地址信息
        if (orderInfo.getAddressId() != null) {
            try {
                Result<Map<String, Object>> addrResult = userClient.getAddressDetail(orderInfo.getAddressId(), buyerId);
                if (addrResult.getCode() == 200 && addrResult.getData() != null) {
                    Map<String, Object> addr = addrResult.getData();
                    orderInfo.setReceiverName((String) addr.get("receiver"));
                    orderInfo.setReceiverPhone((String) addr.get("phone"));
                    String region = (String) addr.getOrDefault("region", "");
                    String detail = (String) addr.getOrDefault("detail", "");
                    orderInfo.setReceiverAddr(region + " " + detail);
                } else {
                    throw new IllegalArgumentException("收货地址不存在或不属于当前用户");
                }
            } catch (Exception e) {
                throw new IllegalStateException("无法获取有效收货地址", e);
            }
        }

        this.save(orderInfo);

        // 2. 再远程扣库存（Redisson 分布式锁保证并发安全）
        Result<String> itemResult;
        try {
            itemResult = itemClient.deductStock(orderInfo.getItemId());
        } catch (Exception e) {
            // Feign 调用异常 → 取消订单，库存未扣，无需补偿
            orderInfo.setStatus(2);
            this.updateById(orderInfo);
            throw new RuntimeException("下单失败，库存服务异常", e);
        }

        if (itemResult.getCode() != 200) {
            // 库存不足/商品已售 → 取消订单，库存未扣，无需补偿
            orderInfo.setStatus(2);
            this.updateById(orderInfo);
            throw new RuntimeException("下单失败：" + itemResult.getMessage());
        }

        // 3. 扔进延时队列 (5分钟未支付自动取消→恢复库存)
        RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue("order:cancel:queue");
        RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
        delayedQueue.offer(orderNo, 5, TimeUnit.MINUTES);

        return orderNo;
    }

    @Override
    public boolean payOrder(String orderNo, Long buyerId) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo).eq("buyer_id", buyerId);
        OrderInfo order = this.getOne(queryWrapper);

        if (order == null) throw new RuntimeException("订单不存在或无权操作");
        if (order.getStatus() != 0) throw new RuntimeException("订单状态异常，无法支付");

        return baseMapper.transitionStatus(orderNo, 0, 1) == 1;
    }

    @Override
    public List<OrderInfo> getMyBoughtOrders(Long buyerId) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("buyer_id", buyerId).ne("status", 3).orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    @Override
    public List<OrderInfo> getMySoldOrders(Long sellerId) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("seller_id", sellerId).ne("status", 3).orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    @Override
    public boolean payOrderByCallback(String orderNo) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);
        OrderInfo order = this.getOne(queryWrapper);

        return order != null && baseMapper.transitionStatus(orderNo, 0, 1) == 1;
    }

    @Override
    public boolean cancelPendingOrder(String orderNo) {
        return baseMapper.transitionStatus(orderNo, 0, 2) == 1;
    }
}
