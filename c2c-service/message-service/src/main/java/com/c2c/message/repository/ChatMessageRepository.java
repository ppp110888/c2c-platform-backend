package com.c2c.message.repository;

import com.c2c.message.entity.ChatMessageLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessageLog, String> {

    //  Spring Data 魔法：只需按命名规则写方法名，它会自动生成查询逻辑！
    // 查找 A 和 B 之间的聊天记录，按时间正序排列
    List<ChatMessageLog> findByFromUserIdAndToUserIdOrFromUserIdAndToUserIdOrderBySendTimeAsc(
            Long from1, Long to1, Long from2, Long to2);
}