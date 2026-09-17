package com.c2c.order.listener;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.c2c.order.entity.OrderInfo;
import com.c2c.order.feign.ItemClient;
import com.c2c.order.service.OrderService;
import jakarta.annotation.Resource;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class OrderCancelListener implements ApplicationRunner {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private OrderService orderService;
    @Resource
    private ItemClient itemClient;

    @Override
    public void run(ApplicationArguments args) {
        new Thread(() -> {
            RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue("order:cancel:queue");

            while (true) {
                try {
                    // 阻塞等待，直到有订单到期
                    String orderNo = blockingQueue.take();
                    System.out.println("🚨 收到过期订单，开始检查状态，单号：" + orderNo);

                    // 1. 去数据库查一下这个订单的最新状态
                    QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("order_no", orderNo);
                    OrderInfo order = orderService.getOne(queryWrapper);

                    if (order != null) {
                        // 2. 判断是否依然是“待支付”状态 (status == 0)
                        if (order.getStatus() == 0 && orderService.cancelPendingOrder(orderNo)) {
                            System.out.println("⚠️ 订单未支付，执行关闭订单并退还库存！");

                            // 3. 把状态改成 2 (已取消)，更新进数据库
                            // 4. 远程调用商品服务，把库存加回去！
                            itemClient.addStock(order.getItemId());

                            System.out.println("✅ 处理完成：订单已关闭，库存已恢复。");
                        } else {
                            System.out.println("ℹ️ 订单状态已变更 (可能已支付)，无需处理。");
                        }
                    }

                } catch (InterruptedException e) {
                    System.out.println("监听线程被中断");
                    break;
                } catch (Exception e) {
                    // 捕获其他异常，防止线程因为报错而彻底死掉
                    System.out.println("❌ 处理过期订单时发生异常：" + e.getMessage());
                }
            }
        }, "Order-Cancel-Thread").start();
    }
}
