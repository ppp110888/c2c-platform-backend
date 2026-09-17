package com.c2c.user.vo;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;

@Data
@Builder // 开启建造者模式，方便拼装对象
public class UserLoginVO implements Serializable {
    private String token;
    private Long userId;
    private String username;
    private String avatar; // 预留头像字段
}