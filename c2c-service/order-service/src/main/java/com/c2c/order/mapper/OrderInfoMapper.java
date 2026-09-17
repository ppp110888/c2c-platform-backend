package com.c2c.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.order.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
    @Update("UPDATE order_info SET status = #{newStatus}, update_time = NOW() " +
            "WHERE order_no = #{orderNo} AND status = #{expectedStatus}")
    int transitionStatus(@Param("orderNo") String orderNo,
                         @Param("expectedStatus") int expectedStatus,
                         @Param("newStatus") int newStatus);
}
