package com.c2c.item.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.Date;

@Data
// 🚨 必须改回 item！我们直接在老表上动手术清洗数据！
@Document(indexName = "item")
public class ItemDoc {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Long)
    private Long sellerId;

    @Field(type = FieldType.Keyword, index = false)
    private String images;

    @Field(type = FieldType.Date)
    private Date createTime;
}