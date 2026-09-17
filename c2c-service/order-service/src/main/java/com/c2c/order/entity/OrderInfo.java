package com.c2c.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_info")
public class OrderInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo; // 订单号
    private Long buyerId; // 买家ID
    private Long sellerId; // 卖家ID
    private Long itemId; // 商品ID
    private String itemTitle; // 商品标题快照
    private String itemImage; // 商品首图快照
    private BigDecimal itemPrice; // 商品价格快照
    private BigDecimal payAmount; // 实付金额
    private Integer status; // 状态(0待支付)
    private Long addressId; // 收货地址ID
    private String receiverName; // 收件人姓名
    private String receiverPhone; // 收件人手机号
    private String receiverAddr; // 完整收货地址
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}