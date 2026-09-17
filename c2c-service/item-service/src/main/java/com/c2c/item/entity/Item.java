package com.c2c.item.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("item")
public class Item {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sellerId; // 卖家ID
    private Long categoryId; // 分类ID
    private String title; // 标题
    private String content; // 描述
    private String images; // 图片
    private BigDecimal price; // 价格
    private BigDecimal originalPrice; // 原价
    private Integer conditionLevel; // 成色
    private Integer status; // 状态(0在售)
    private Integer stock; // 库存
    private String city; // 城市
    private Integer viewCount; // 浏览量
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}