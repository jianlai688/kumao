package com.kumao.search.repository;

import com.kumao.search.entity.SearchDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchDocRepository extends ElasticsearchRepository<SearchDoc, String> {
}
