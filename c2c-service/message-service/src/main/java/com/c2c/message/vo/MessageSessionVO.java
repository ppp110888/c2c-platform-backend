package com.c2c.message.vo;

import lombok.Data;
import java.util.Date;

@Data
public class MessageSessionVO {
    private Long toUserId;       // 对方的用户ID
    private String nickname;     // 对方的昵称
    private String avatar;       // 对方的头像
    private String lastMsg;      // 最后一条消息内容
    private Date lastTime;       // 最后一条消息的时间
    private Integer unreadCount; // 未读消息数量
}