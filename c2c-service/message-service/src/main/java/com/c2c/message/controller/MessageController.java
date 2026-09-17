package com.c2c.message.controller;

import com.c2c.common.result.Result;
import com.c2c.message.entity.ChatMessageLog;
import com.c2c.message.feign.UserClient;
import com.c2c.message.vo.MessageSessionVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/message")
public class MessageController {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private UserClient userClient;

    // 1. 获取消息大厅列表 (保持不变)
    @GetMapping("/session/list")
    public Result<List<MessageSessionVO>> getSessionList(@RequestHeader("X-User-Id") Long currentUserId) {
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("fromUserId").is(currentUserId),
                Criteria.where("toUserId").is(currentUserId)
        ).and("hiddenFor").ne(currentUserId);

        Query query = new Query(criteria).with(Sort.by(Sort.Direction.DESC, "sendTime"));
        List<ChatMessageLog> logs = mongoTemplate.find(query, ChatMessageLog.class);

        Map<Long, MessageSessionVO> sessionMap = new LinkedHashMap<>();

        for (ChatMessageLog logItem : logs) {
            Long otherUserId = logItem.getFromUserId().equals(currentUserId) ? logItem.getToUserId() : logItem.getFromUserId();

            if (!sessionMap.containsKey(otherUserId)) {
                MessageSessionVO vo = new MessageSessionVO();
                vo.setToUserId(otherUserId);
                vo.setLastMsg(logItem.getContent());
                vo.setLastTime(logItem.getSendTime());
                vo.setUnreadCount(0);

                try {
                    Result<Map<String, Object>> userRes = userClient.getPublicInfo(otherUserId);
                    if (userRes != null && userRes.getData() != null) {
                        vo.setNickname((String) userRes.getData().get("nickname"));
                        vo.setAvatar((String) userRes.getData().get("avatar"));
                    } else {
                        vo.setNickname("神秘用户_" + otherUserId);
                    }
                } catch (Exception e) {
                    log.error("获取用户真实信息失败，对方ID: {}", otherUserId, e);
                    vo.setNickname("神秘用户_" + otherUserId);
                }
                sessionMap.put(otherUserId, vo);
            }

            if (logItem.getToUserId().equals(currentUserId) && logItem.getStatus() == 0) {
                MessageSessionVO vo = sessionMap.get(otherUserId);
                vo.setUnreadCount(vo.getUnreadCount() + 1);
            }
        }
        return Result.success(new ArrayList<>(sessionMap.values()));
    }

    // ====================================================================
    // 🚨 2. 新增：获取跟某个人的 1v1 聊天记录 (MongoDB 专用版)
    // ====================================================================
    @GetMapping("/history")
    public Result<List<ChatMessageLog>> getHistory(
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestParam("targetId") Long targetId) {

        // 1. 查找 A发给B 或 B发给A 的所有消息，按时间正序排列
        Criteria criteria = new Criteria().orOperator(
                new Criteria().andOperator(Criteria.where("fromUserId").is(currentUserId), Criteria.where("toUserId").is(targetId)),
                new Criteria().andOperator(Criteria.where("fromUserId").is(targetId), Criteria.where("toUserId").is(currentUserId))
        ).and("hiddenFor").ne(currentUserId);
        Query query = new Query(criteria).with(Sort.by(Sort.Direction.ASC, "sendTime"));
        List<ChatMessageLog> historyList = mongoTemplate.find(query, ChatMessageLog.class);

        // 2. 顺手牵羊：既然你点进来了，就把对方发给你的未读消息全部标记为已读！
        Query updateQuery = new Query(new Criteria().andOperator(
                Criteria.where("fromUserId").is(targetId),
                Criteria.where("toUserId").is(currentUserId),
                Criteria.where("status").is(0)
        ));
        Update update = new Update().set("status", 1);
        mongoTemplate.updateMulti(updateQuery, update, ChatMessageLog.class);

        return Result.success(historyList);
    }

    // ====================================================================
    // 🚨 3. 新增：长按删除聊天会话 (清空你与该用户的聊天记录)
    // ====================================================================
    @DeleteMapping("/session/delete")
    public Result<String> deleteSession(
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestParam("targetId") Long targetId) {

        // 查找 A发给B 或 B发给A 的所有消息
        Criteria criteria = new Criteria().orOperator(
                new Criteria().andOperator(Criteria.where("fromUserId").is(currentUserId), Criteria.where("toUserId").is(targetId)),
                new Criteria().andOperator(Criteria.where("fromUserId").is(targetId), Criteria.where("toUserId").is(currentUserId))
        );
        Query query = new Query(criteria);

        mongoTemplate.updateMulti(query, new Update().addToSet("hiddenFor", currentUserId), ChatMessageLog.class);

        return Result.success("聊天记录已清空");
    }
}
