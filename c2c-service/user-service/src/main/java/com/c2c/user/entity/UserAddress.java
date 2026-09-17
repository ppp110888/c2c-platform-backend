package com.c2c.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_address")
public class UserAddress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;       // 所属用户ID
    private String receiver;   // 收件人姓名
    private String phone;      // 手机号
    private String region;     // 省市区
    private String detail;     // 详细地址
    private Integer isDefault; // 是否默认 (0-否, 1-是)
    private Date createTime;   // 创建时间
}