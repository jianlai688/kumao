package com.kumao.common.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页查询参数
 */
@Data
public class PageQuery implements Serializable {

    private long pageNum = 1;
    private long pageSize = 10;
    private String keyword;

    public <T> Page<T> toPage() {
        long current = pageNum <= 0 ? 1 : pageNum;
        long size = pageSize <= 0 ? 10 : Math.min(pageSize, 100);
        return new Page<>(current, size);
    }
}
