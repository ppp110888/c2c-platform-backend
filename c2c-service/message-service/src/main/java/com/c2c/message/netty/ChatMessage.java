package com.c2c.message.netty;

import lombok.Data;

@Data
public class ChatMessage {
    // 消息类型：1表示"认证"(绑定ID)，2表示"聊天"(发给别人)
    private Integer type;

    // 认证用的 JWT Token (类型1时有效)
    private String token;

    // 要发给谁的 userId (类型2时有效)
    private Long toUserId;

    // 聊天内容
    private String content;
}