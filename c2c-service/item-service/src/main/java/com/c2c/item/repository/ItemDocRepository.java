package com.c2c.item.repository;

import com.c2c.item.document.ItemDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 只要继承了这个接口，增删改查 ES 就和用 MyBatis-Plus 一样简单！
 */
@Repository
public interface ItemDocRepository extends ElasticsearchRepository<ItemDoc, Long> {
}