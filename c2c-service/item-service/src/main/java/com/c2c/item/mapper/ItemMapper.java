package com.c2c.item.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.item.entity.Item;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ItemMapper extends BaseMapper<Item> {
}