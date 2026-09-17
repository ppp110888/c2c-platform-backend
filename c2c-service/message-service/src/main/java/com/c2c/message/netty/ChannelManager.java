package com.c2c.message.netty;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ChannelManager {
    // 核心内存字典：存储 userId 和对应的 WebSocket 通道
    private static final ConcurrentHashMap<Long, Channel> USER_CHANNELS = new ConcurrentHashMap<>();

    // 1. 用户上线，绑定通道
    public static void addChannel(Long userId, Channel channel) {
        USER_CHANNELS.put(userId, channel);
        log.info("👤 用户 [{}] 上线了，当前在线总人数: {}", userId, USER_CHANNELS.size());
    }

    // 2. 用户下线，移除通道
    public static void removeChannel(Channel channel) {
        // 遍历找到对应的 userId 并移除
        USER_CHANNELS.entrySet().removeIf(entry -> {
            if (entry.getValue() == channel) {
                log.info("👤 用户 [{}] 下线了", entry.getKey());
                return true;
            }
            return false;
        });
    }

    // 3. 根据 userId 查找通道 (用于精准发消息)
    public static Channel getChannel(Long userId) {
        return USER_CHANNELS.get(userId);
    }

    // 🚨 4. 【新增】根据 Channel 反向查找 userId (用于写聊天记录时知道是谁发的)
    public static Long getUserIdByChannel(Channel channel) {
        for (Map.Entry<Long, Channel> entry : USER_CHANNELS.entrySet()) {
            if (entry.getValue() == channel) {
                return entry.getKey(); // 找到了，返回对应的用户 ID
            }
        }
        return null; // 找不到说明这个通道还没完成身份认证
    }
}