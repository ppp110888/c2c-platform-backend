package com.c2c.order.service.impl;

import com.c2c.common.result.Result;
import com.c2c.order.entity.OrderInfo;
import com.c2c.order.feign.ItemClient;
import com.c2c.order.feign.UserClient;
import com.c2c.order.mapper.OrderInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock ItemClient itemClient;
    @Mock UserClient userClient;
    @Mock RedissonClient redissonClient;
    @Mock OrderInfoMapper mapper;
    @Mock RBlockingQueue<String> blockingQueue;
    @Mock RDelayedQueue<String> delayedQueue;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl();
        ReflectionTestUtils.setField(service, "itemClient", itemClient);
        ReflectionTestUtils.setField(service, "userClient", userClient);
        ReflectionTestUtils.setField(service, "redissonClient", redissonClient);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void createOrderUsesAuthoritativePriceAndSeller() {
        when(itemClient.getItemInfo(10L)).thenReturn(Result.success(Map.of(
                "title", "Item", "image", "image.jpg", "price", new BigDecimal("88.50"),
                "status", 0, "sellerId", 9L)));
        when(userClient.getAddressDetail(20L, 3L)).thenReturn(Result.success(Map.of(
                "receiver", "Buyer", "phone", "10086", "region", "Guangdong", "detail", "Guangzhou")));
        when(mapper.insert(any(OrderInfo.class))).thenReturn(1);
        when(itemClient.deductStock(10L)).thenReturn(Result.success("ok"));
        when(redissonClient.<String>getBlockingQueue(anyString())).thenReturn(blockingQueue);
        when(redissonClient.getDelayedQueue(blockingQueue)).thenReturn(delayedQueue);

        OrderInfo order = new OrderInfo();
        order.setItemId(10L);
        order.setAddressId(20L);
        order.setSellerId(999L);
        order.setPayAmount(new BigDecimal("0.01"));

        service.createOrder(3L, order);

        assertEquals(9L, order.getSellerId());
        assertEquals(new BigDecimal("88.50"), order.getPayAmount());
        verify(mapper).insert(order);
    }

    @Test
    void createOrderRejectsAddressOutsideBuyerAccount() {
        when(itemClient.getItemInfo(10L)).thenReturn(Result.success(Map.of(
                "title", "Item", "image", "image.jpg", "price", new BigDecimal("88.50"),
                "status", 0, "sellerId", 9L)));
        when(userClient.getAddressDetail(20L, 3L)).thenReturn(Result.failed(404, "not found"));

        OrderInfo order = new OrderInfo();
        order.setItemId(10L);
        order.setAddressId(20L);

        assertThrows(IllegalStateException.class, () -> service.createOrder(3L, order));
    }
}
