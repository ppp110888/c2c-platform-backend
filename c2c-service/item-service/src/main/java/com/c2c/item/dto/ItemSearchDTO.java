package com.c2c.item.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ItemSearchDTO {
    private String keyword;     // 搜索关键词 (如 "索尼耳机")
    private BigDecimal minPrice; // 最低价格
    private BigDecimal maxPrice; // 最高价格
    private Integer pageNum = 1; // 当前页码 (默认第1页)
    private Integer pageSize = 10; // 每页条数 (默认10条)
}