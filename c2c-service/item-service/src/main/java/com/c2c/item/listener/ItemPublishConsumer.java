package com.c2c.item.listener;

import com.c2c.item.service.ItemService;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
// 🚨 核心注解：告诉 RocketMQ 我要听哪个群的消息
@RocketMQMessageListener(
        topic = "item-topic",          // 监听的主题，必须和发送端一模一样！
        consumerGroup = "item-consumer-group", // 消费者组名称
        selectorExpression = "publish" // 🚨 关键：只听 tag 为 publish（发布）的消息
)
public class ItemPublishConsumer implements RocketMQListener<Long> {

    @Resource
    private ItemService itemService;

    @Override
    public void onMessage(Long itemId) {
        System.out.println("📥 【RocketMQ 消费者】侦测到新发布的商品！收到商品 ID: " + itemId);
        try {
            // =======================================================
            // 🚨 联动：调用你 Service 里单条商品同步 ES 的方法
            // (如果没有单条同步方法，这里也可以暂时调用你已有的 itemService.syncItemsToEs() 刷全量)
            // =======================================================
            itemService.syncItemsToEs();

            System.out.println("🎉 自动化奇迹！商品 ID: " + itemId + " 的数据已全自动同步至 ES 引擎！");
        } catch (Exception e) {
            System.err.println("❌ 异步同步 ES 失败，原因: " + e.getMessage());
            // 💡 架构师高阶机密：这里只要抛出异常，RocketMQ 就会认为消费失败，
            // 它会在后台自动发起最多 16 次的“夺命连环重试”，直到同步成功为止！
            throw new RuntimeException("同步ES失败，触发MQ重试机制", e);
        }
    }
}