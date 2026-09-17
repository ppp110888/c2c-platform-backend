package com.c2c.item.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.c2c.common.result.Result;
import com.c2c.item.entity.Item;
import java.util.List;

public interface ItemService extends IService<Item> {
    // 获取首页商品列表
    List<Item> getHomeItemList();

    // 🚨 新增：将 MySQL 数据全量同步到 ES
    void syncItemsToEs();

    // 🚨 新增：基于 ES 的分页、分词、高亮搜索接口
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.c2c.item.vo.ItemSearchVO> search(com.c2c.item.dto.ItemSearchDTO searchDTO);

    // ==========================================
    // 🚨 下面是新增的：防超卖扣库存 & 恢复库存接口
    // ==========================================

    // 扣减库存 (加分布式锁)
    Result<String> deductStock(Long itemId);

    // 恢复库存
    Result<String> addStock(Long itemId);
}