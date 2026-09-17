package com.c2c.message.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@Document(collection = "chat_message_log") // 指定 MongoDB 里的集合名
public class ChatMessageLog {

    @Id
    private String id;          // Mongo 的主键通常是 String 类型的 ObjectId

    private Long fromUserId;    // 谁发的
    private Long toUserId;      // 发给谁
    private String content;     // 消息内容
    private Date sendTime;      // 发送时间
    private Integer status;     // 状态 (0: 未读, 1: 已读)
    private Set<Long> hiddenFor = new HashSet<>();
}
