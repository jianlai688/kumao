package com.kumao.search.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

/**
 * 启动时创建 ES 索引（ES 不可用时只记录告警，服务仍可启动并降级）
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EsIndexInitializer {

    private final ElasticsearchOperations operations;

    @Value("${kumao.search.index:kumao_search}")
    private String indexName;

    @Bean
    public ApplicationRunner initIndex() {
        return args -> {
            try {
                IndexOperations indexOps = operations.indexOps(com.kumao.search.entity.SearchDoc.class);
                if (!indexOps.exists()) {
                    indexOps.createWithMapping();
                    log.info("ES 索引 {} 创建成功", indexName);
                }
            } catch (Exception e) {
                log.warn("ES 索引初始化失败，检索将降级到 MySQL: {}", e.getMessage());
            }
        };
    }
}
