package com.kumao.search.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;

/**
 * ES 统一检索文档：歌曲/歌手/歌单/直播间/音乐人
 */
@Data
@Document(indexName = "kumao_search", createIndex = false)
public class SearchDoc implements Serializable {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long bizId;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String subtitle;

    @Field(type = FieldType.Keyword, index = false)
    private String coverUrl;

    @Field(type = FieldType.Keyword)
    private String tags;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Long)
    private Long heat;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private java.time.ZonedDateTime createTime;

    public String docId() {
        return type + "_" + bizId;
    }
}
