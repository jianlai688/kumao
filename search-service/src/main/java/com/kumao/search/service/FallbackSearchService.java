package com.kumao.search.service;

import com.kumao.search.dto.SearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * ES 不可用时的 MySQL 降级检索（LIKE），保证演示环境可用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FallbackSearchService {

    private final JdbcTemplate jdbcTemplate;

    public SearchResultVO search(String keyword, String type, int pageNum, int pageSize) {
        SearchResultVO vo = new SearchResultVO();
        vo.setKeyword(keyword);
        if (!StringUtils.hasText(keyword)) {
            return vo;
        }
        String kw = "%" + keyword.trim() + "%";
        int offset = (Math.max(1, pageNum) - 1) * pageSize;
        List<SearchResultVO.Item> items = new ArrayList<>();
        String t = type == null ? "" : type.toUpperCase();

        if (t.isEmpty() || "SONG".equals(t)) {
            items.addAll(jdbcTemplate.query(
                    "select id, name, singer_name, cover_url, style, language from song " +
                            "where deleted = 0 and status = 1 and (name like ? or singer_name like ?) order by play_count desc limit ? offset ?",
                    (rs, i) -> item(rs.getLong("id"), "SONG", rs.getString("name"),
                            rs.getString("singer_name"), rs.getString("cover_url"),
                            rs.getString("style"), rs.getString("language"), 0L),
                    kw, kw, pageSize, offset));
        }
        if (t.isEmpty() || "SINGER".equals(t)) {
            items.addAll(jdbcTemplate.query(
                    "select id, name, region, avatar from singer where deleted = 0 and name like ? limit ? offset ?",
                    (rs, i) -> item(rs.getLong("id"), "SINGER", rs.getString("name"),
                            rs.getString("region"), rs.getString("avatar"), rs.getString("region"), null, 0L),
                    kw, pageSize, offset));
        }
        if (t.isEmpty() || "PLAYLIST".equals(t)) {
            items.addAll(jdbcTemplate.query(
                    "select id, name, creator_name, cover_url, description from playlist " +
                            "where deleted = 0 and is_public = 1 and name like ? limit ? offset ?",
                    (rs, i) -> item(rs.getLong("id"), "PLAYLIST", rs.getString("name"),
                            rs.getString("creator_name"), rs.getString("cover_url"),
                            null, rs.getString("description"), 0L),
                    kw, pageSize, offset));
        }
        if (t.isEmpty() || "LIVE_ROOM".equals(t)) {
            items.addAll(jdbcTemplate.query(
                    "select id, title, anchor_name, cover_url, category from live_room " +
                            "where deleted = 0 and status = 'LIVING' and (title like ? or anchor_name like ?) limit ? offset ?",
                    (rs, i) -> item(rs.getLong("id"), "LIVE_ROOM", rs.getString("title"),
                            rs.getString("anchor_name"), rs.getString("cover_url"),
                            rs.getString("category"), null, 0L),
                    kw, kw, pageSize, offset));
        }
        if (t.isEmpty() || "MUSICIAN".equals(t)) {
            items.addAll(jdbcTemplate.query(
                    "select id, stage_name, style, avatar, intro, fans_count from musician " +
                            "where deleted = 0 and status = 'APPROVED' and (stage_name like ? or style like ?) limit ? offset ?",
                    (rs, i) -> item(rs.getLong("id"), "MUSICIAN", rs.getString("stage_name"),
                            rs.getString("style"), rs.getString("avatar"),
                            rs.getString("style"), rs.getString("intro"), rs.getLong("fans_count")),
                    kw, kw, pageSize, offset));
        }
        vo.setTotal(items.size());
        vo.setItems(items);
        return vo;
    }

    private SearchResultVO.Item item(Long id, String type, String title, String subtitle,
                                     String cover, String tags, String description, Long heat) {
        SearchResultVO.Item item = new SearchResultVO.Item();
        item.setBizId(id);
        item.setType(type);
        item.setTitle(title);
        item.setSubtitle(subtitle);
        item.setCoverUrl(cover);
        item.setTags(tags);
        item.setDescription(description);
        item.setHeat(heat == null ? 0L : heat);
        return item;
    }
}
