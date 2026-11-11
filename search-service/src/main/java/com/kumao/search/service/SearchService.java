package com.kumao.search.service;

import com.kumao.api.dto.SearchDocDTO;
import com.kumao.search.dto.SearchResultVO;
import com.kumao.search.entity.SearchDoc;
import com.kumao.search.repository.SearchDocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchDocRepository repository;
    private final ElasticsearchOperations operations;
    private final FallbackSearchService fallbackSearchService;

    @Value("${kumao.search.fallback:true}")
    private boolean fallback;

    public void sync(SearchDocDTO dto) {
        if (dto == null || dto.getBizId() == null || !StringUtils.hasText(dto.getType())) {
            return;
        }
        repository.save(toDoc(dto));
    }

    public void delete(String type, Long bizId) {
        repository.deleteById(type + "_" + bizId);
    }

    /**
     * 关键词检索歌曲、歌手、歌单、直播间、音乐人（标题/副标题/标签/描述）
     */
    public SearchResultVO search(String keyword, String type, int pageNum, int pageSize) {
        int size = Math.max(1, Math.min(pageSize, 100));
        int page = Math.max(0, pageNum - 1);
        if (!StringUtils.hasText(keyword)) {
            SearchResultVO empty = new SearchResultVO();
            empty.setKeyword(keyword);
            return empty;
        }
        try {
            Criteria criteria = new Criteria("title").matches(keyword)
                    .or(new Criteria("subtitle").matches(keyword))
                    .or(new Criteria("tags").matches(keyword))
                    .or(new Criteria("description").matches(keyword));
            CriteriaQuery query = new CriteriaQuery(criteria, PageRequest.of(page, size));
            if (StringUtils.hasText(type)) {
                query.addCriteria(new Criteria("type").is(type.toUpperCase()));
            }

            SearchHits<SearchDoc> hits = operations.search(query, SearchDoc.class);
            if (hits.getTotalHits() == 0 && fallback) {
                // ES 分词对中文支持有限，无命中时兜底 MySQL LIKE，保证演示可用
                SearchResultVO fallbackResult = fallbackSearchService.search(keyword, type, pageNum, size);
                if (!fallbackResult.getItems().isEmpty()) {
                    return fallbackResult;
                }
            }
            SearchResultVO vo = new SearchResultVO();
            vo.setKeyword(keyword);
            vo.setTotal(hits.getTotalHits());
            List<SearchResultVO.Item> items = new ArrayList<>();
            for (SearchHit<SearchDoc> hit : hits.getSearchHits()) {
                SearchResultVO.Item item = toItem(hit.getContent());
                // 关键词高亮（统一 <em> 标签，前端直接渲染）
                item.setTitle(highlight(item.getTitle(), keyword));
                item.setSubtitle(highlight(item.getSubtitle(), keyword));
                items.add(item);
            }
            vo.setItems(items);
            return vo;
        } catch (Exception e) {
            log.warn("ES 检索失败，降级 MySQL: {}", e.getMessage());
            if (fallback) {
                return fallbackSearchService.search(keyword, type, pageNum, size);
            }
            throw new com.kumao.common.exception.BizException("检索服务暂不可用，请稍后再试");
        }
    }

    /**
     * 检索数据监控
     */
    public Map<String, Object> monitor() {
        Map<String, Object> map = new HashMap<>();
        map.put("index", "kumao_search");
        try {
            map.put("docCount", repository.count());
            map.put("esAvailable", true);
        } catch (Exception e) {
            map.put("docCount", 0);
            map.put("esAvailable", false);
            map.put("message", String.valueOf(e.getMessage()));
        }
        return map;
    }

    public void saveAll(List<SearchDocDTO> docs) {
        if (docs == null || docs.isEmpty()) {
            return;
        }
        repository.saveAll(docs.stream().map(this::toDoc).collect(Collectors.toList()));
    }

    private String highlight(String text, String keyword) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(keyword)) {
            return text;
        }
        return text.replace(keyword, "<em>" + keyword + "</em>");
    }

    private SearchDoc toDoc(SearchDocDTO dto) {
        SearchDoc doc = new SearchDoc();
        doc.setId(dto.getType() + "_" + dto.getBizId());
        doc.setBizId(dto.getBizId());
        doc.setType(dto.getType());
        doc.setTitle(dto.getTitle());
        doc.setSubtitle(dto.getSubtitle());
        doc.setCoverUrl(dto.getCoverUrl());
        doc.setTags(dto.getTags());
        doc.setDescription(dto.getDescription());
        doc.setHeat(dto.getHeat() == null ? 0L : dto.getHeat());
        doc.setCreateTime(ZonedDateTime.now(ZoneId.systemDefault()));
        return doc;
    }

    private SearchResultVO.Item toItem(SearchDoc doc) {
        SearchResultVO.Item item = new SearchResultVO.Item();
        item.setBizId(doc.getBizId());
        item.setType(doc.getType());
        item.setTitle(doc.getTitle());
        item.setSubtitle(doc.getSubtitle());
        item.setCoverUrl(doc.getCoverUrl());
        item.setTags(doc.getTags());
        item.setDescription(doc.getDescription());
        item.setHeat(doc.getHeat());
        return item;
    }
}
