package com.c2c.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user") // 你的表名是 user
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    // 🚨 新增：用户的“门面”字段
    private String nickname; // 昵称

    private String bio;

    private String avatar;   // 头像URL (将来可以存咱们 MinIO 上传的图片地址)

    // 顺手加上这两个实用的业务字段（非必填，但建议留着备用）
    private String phone;    // 手机号
    private Integer status;  // 状态：1正常，0冻结

    private LocalDateTime createTime;
}