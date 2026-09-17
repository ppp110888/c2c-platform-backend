package com.c2c.message.netty;

import cn.hutool.json.JSONUtil;
import com.c2c.common.utils.JwtUtils;
import com.c2c.message.entity.ChatMessageLog;
import com.c2c.message.repository.ChatMessageRepository;
import io.jsonwebtoken.Claims;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Date;

@Slf4j
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    // 🚨 1. 声明 Mongo 操作类
    private final ChatMessageRepository chatMessageRepository;

    // 🚨 2. 添加构造函数，接收 NettyServer 传过来的大礼包
    public ChatHandler(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
        String jsonText = msg.text();
        Channel currentChannel = ctx.channel();

        try {
            ChatMessage chatMsg = JSONUtil.toBean(jsonText, ChatMessage.class);

            // 认证逻辑保持不变
            if (chatMsg.getType() == 1) {
                String token = chatMsg.getToken();
                if (!StringUtils.hasText(token)) {
                    currentChannel.writeAndFlush(new TextWebSocketFrame("系统: 认证失败，未携带Token"));
                    return;
                }
                if (token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }
                Claims claims = JwtUtils.parseToken(token);
                Long userId = claims.get("userId", Long.class);

                // 🚨 改进：把 userId 存入当前 Channel 的属性里，方便等下发消息时知道是谁发的！
                ChannelManager.addChannel(userId, currentChannel);
                currentChannel.writeAndFlush(new TextWebSocketFrame("系统: 认证成功！欢迎您，用户 " + userId));
            }

            // 聊天逻辑：投递 + 落库
            else if (chatMsg.getType() == 2) {
                Long toUserId = chatMsg.getToUserId();
                String content = chatMsg.getContent();

                // 🚨 3. 从 ChannelManager 里反向查找当前发消息的人的 ID (fromUserId)
                Long fromUserId = ChannelManager.getUserIdByChannel(currentChannel);
                if (fromUserId == null) {
                    currentChannel.writeAndFlush(new TextWebSocketFrame("系统: 请先完成认证！"));
                    return;
                }

                // 🚨 4. 构建并保存聊天记录到 MongoDB
                ChatMessageLog logEntity = new ChatMessageLog();
                logEntity.setFromUserId(fromUserId);
                logEntity.setToUserId(toUserId);
                logEntity.setContent(content);
                logEntity.setSendTime(new Date());
                logEntity.setStatus(0); // 0代表未读
                chatMessageRepository.save(logEntity); // 一行代码直接落库！

                // 继续之前的投递逻辑
                Channel targetChannel = ChannelManager.getChannel(toUserId);
                if (targetChannel != null && targetChannel.isActive()) {
                    targetChannel.writeAndFlush(new TextWebSocketFrame("新消息: " + content));
                    log.info("消息投递成功: [{}] 发给 [{}]，内容: {}", fromUserId, toUserId, content);
                } else {
                    currentChannel.writeAndFlush(new TextWebSocketFrame("系统: 对方不在线，已转为离线消息"));
                    log.info("消息投递失败: 用户 [{}] 不在线，已存入离线数据库", toUserId);
                }
            }

        } catch (Exception e) {
            log.error("处理消息异常", e);
            currentChannel.writeAndFlush(new TextWebSocketFrame("系统: 消息格式不正确或Token无效"));
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        ChannelManager.removeChannel(ctx.channel());
    }
}