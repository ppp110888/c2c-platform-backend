package com.c2c.item.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ItemSearchVO {
    private Long id;
    private String title;       // 🚨 注意：这里返回的可能是带有 <em> 标签的高亮标题！
    private String content;     // 🚨 同上
    private BigDecimal price;
    private String city;
    private String coverImage;

    // 🚨 终极补丁：必须在 VO 里加上卖家ID，否则前端收不到！
    private Long sellerId;

    // 🚨 终极补丁：必须在 VO 里加上图片，否则前端又是风景图！
    private String images;
}