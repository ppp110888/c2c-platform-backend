package com.c2c.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.c2c.common.result.Result;
import com.c2c.item.document.ItemDoc;
import com.c2c.item.dto.ItemSearchDTO;
import com.c2c.item.entity.Item;
import com.c2c.item.mapper.ItemMapper;
import com.c2c.item.repository.ItemDocRepository;
import com.c2c.item.service.ItemService;
import com.c2c.item.vo.ItemSearchVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements ItemService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ItemDocRepository itemDocRepository;

    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private org.apache.rocketmq.spring.core.RocketMQTemplate rocketMQTemplate;

    @Resource
    private ObjectMapper objectMapper;

    private static final String HOME_ITEM_LIST_KEY = "item:home:list";

    @Override
    @SuppressWarnings("unchecked")
    public List<Item> getHomeItemList() {
        List<Item> cachedList = (List<Item>) redisTemplate.opsForValue().get(HOME_ITEM_LIST_KEY);

        if (cachedList != null && !cachedList.isEmpty()) {
            System.out.println("⚡️ 命中 Redis 缓存，直接返回！");
            return cachedList;
        }

        System.out.println("🐢 未命中缓存，查询 MySQL 数据库...");
        List<Item> dbList = this.list(new LambdaQueryWrapper<Item>()
                .eq(Item::getStatus, 0)
                .orderByDesc(Item::getCreateTime)
                .last("LIMIT 10"));

        if (dbList != null && !dbList.isEmpty()) {
            redisTemplate.opsForValue().set(HOME_ITEM_LIST_KEY, dbList, 5, TimeUnit.MINUTES);
        }

        return dbList;
    }

    @Override
    public void syncItemsToEs() {
        log.info("开始执行 MySQL 到 ES 的全量数据同步...");
        List<Item> itemList = this.lambdaQuery().eq(Item::getStatus, 0).list();

        if (itemList == null || itemList.isEmpty()) {
            log.info("MySQL 里没有需要同步的商品数据。");
            return;
        }

        // =======================================================
        // 🚨 核弹级洗地：先把 ES 里面残缺不全的旧垃圾数据全部删光！
        // =======================================================
        itemDocRepository.deleteAll();

        List<ItemDoc> itemDocList = itemList.stream().map(item -> {
            ItemDoc doc = new ItemDoc();
            BeanUtils.copyProperties(item, doc);

            // 手动补全最重要的两个字段
            doc.setSellerId(item.getSellerId());
            doc.setImages(item.getImages());
            return doc;
        }).collect(Collectors.toList());

        itemDocRepository.saveAll(itemDocList);
        log.info("🎉 数据同步完成！已成功将 {} 条完美数据灌入 ES！", itemDocList.size());
    }

    @Override
    public Page<ItemSearchVO> search(ItemSearchDTO dto) {
        log.info("🔍 收到商品搜索请求，关键词: [{}], 价格区间: [{}-{}]", dto.getKeyword(), dto.getMinPrice(), dto.getMaxPrice());

        int pageNum = dto.getPageNum() == null ? 1 : Math.max(1, dto.getPageNum());
        int pageSize = dto.getPageSize() == null ? 10 : Math.min(50, Math.max(1, dto.getPageSize()));
        boolean hasKeyword = StringUtils.hasText(dto.getKeyword());
        boolean hasPriceFilter = dto.getMinPrice() != null || dto.getMaxPrice() != null;

        // 既没关键词又没价格筛选，返回空
        if (!hasKeyword && !hasPriceFilter) {
            return new Page<>(pageNum, pageSize);
        }

        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize);

        HighlightFieldParameters parameters = HighlightFieldParameters.builder()
                .withPreTags("<em style='color:red;'>")
                .withPostTags("</em>")
                .build();
        HighlightField titleField = new HighlightField("title", parameters);
        HighlightField contentField = new HighlightField("content", parameters);
        Highlight highlight = new Highlight(Arrays.asList(titleField, contentField));
        HighlightQuery highlightQuery = new HighlightQuery(highlight, null);

        // 构建 bool 查询 DSL
        StringBuilder dsl = new StringBuilder("{ \"bool\": { ");

        // must 子句
        dsl.append("\"must\": [");
        if (hasKeyword) {
            try {
                dsl.append("{ \"multi_match\": { \"query\": ")
                   .append(objectMapper.writeValueAsString(dto.getKeyword()))
                   .append(", \"fields\": [\"title\", \"content\"] } }");
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("搜索关键词无效", e);
            }
        } else {
            dsl.append("{ \"match_all\": {} }");
        }
        dsl.append("]");

        // filter 子句 — 价格区间
        if (hasPriceFilter) {
            dsl.append(", \"filter\": [");
            dsl.append("{ \"range\": { \"price\": { ");
            if (dto.getMinPrice() != null) {
                dsl.append("\"gte\": ").append(dto.getMinPrice());
            }
            if (dto.getMaxPrice() != null) {
                if (dto.getMinPrice() != null) dsl.append(", ");
                dsl.append("\"lte\": ").append(dto.getMaxPrice());
            }
            dsl.append(" } } }");
            dsl.append("]");
        }

        dsl.append(" } }");

        StringQuery searchQuery = new StringQuery(dsl.toString());
        searchQuery.setPageable(pageRequest);
        searchQuery.setHighlightQuery(highlightQuery);

        SearchHits<ItemDoc> searchHits = elasticsearchOperations.search(searchQuery, ItemDoc.class);

        List<ItemSearchVO> voList = new ArrayList<>();
        for (SearchHit<ItemDoc> hit : searchHits) {
            ItemDoc doc = hit.getContent();
            ItemSearchVO vo = new ItemSearchVO();
            BeanUtils.copyProperties(doc, vo);

            List<String> titleHighlights = hit.getHighlightFields().get("title");
            if (titleHighlights != null && !titleHighlights.isEmpty()) {
                vo.setTitle(titleHighlights.get(0));
            }

            List<String> contentHighlights = hit.getHighlightFields().get("content");
            if (contentHighlights != null && !contentHighlights.isEmpty()) {
                vo.setContent(contentHighlights.get(0));
            }

            voList.add(vo);
        }

        Page<ItemSearchVO> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setRecords(voList);
        resultPage.setTotal(searchHits.getTotalHits());

        return resultPage;
    }

    @Override
    public Result<String> deductStock(Long itemId) {
        String lockKey = "lock:item:stock:" + itemId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                return Result.failed("当前购买人数过多，请稍后再试！");
            }

            Item item = this.getById(itemId);
            if (item == null) {
                return Result.failed("商品不存在");
            }

            if (item.getStatus() != 0) {
                return Result.failed("手慢了，商品已被抢光或下架！");
            }

            item.setStatus(2);
            // 扣减库存数量（兼容 stock 为 null 的情况）
            if (item.getStock() != null && item.getStock() > 0) {
                item.setStock(item.getStock() - 1);
            }
            this.updateById(item);

            // 清除首页 Redis 缓存，让修改立即生效
            redisTemplate.delete(HOME_ITEM_LIST_KEY);
            // 从 ES 中移除已售商品，防止搜索结果里出现
            itemDocRepository.deleteById(item.getId());

            log.info("✅ 商品 {} 已锁定，库存-1，缓存已清除，ES已同步", itemId);
            return Result.success("锁定商品成功！");

        } catch (InterruptedException e) {
            log.error("扣减库存加锁异常", e);
            return Result.failed("系统繁忙");
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public Result<String> addStock(Long itemId) {
        String lockKey = "lock:item:stock:" + itemId;
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock(10, TimeUnit.SECONDS);
        try {
            Item item = this.getById(itemId);
            if (item != null && item.getStatus() == 2) {
            item.setStatus(0);
            // 恢复库存数量
            if (item.getStock() != null) {
                item.setStock(item.getStock() + 1);
            }
            this.updateById(item);

            // 清除首页 Redis 缓存
            redisTemplate.delete(HOME_ITEM_LIST_KEY);
            // 把商品重新加回 ES，恢复可搜索
            ItemDoc doc = new ItemDoc();
            BeanUtils.copyProperties(item, doc);
            doc.setSellerId(item.getSellerId());
            doc.setImages(item.getImages());
            itemDocRepository.save(doc);

            log.info("🔄 商品 {} 库存已恢复，缓存已清除，已加回ES", itemId);
            }
            return Result.success("库存恢复成功");
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }
}
