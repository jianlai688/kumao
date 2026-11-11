package com.kumao.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_record")
public class FileRecord extends BaseEntity {

    private String originalName;
    private String objectKey;
    private String url;
    private String bucket;
    private String contentType;
    private Long size;
    /** IMAGE / AUDIO / VIDEO / LYRIC / OTHER */
    private String fileType;
    private String bizType;
    private Long uploaderId;
    private Integer deleted;
}
