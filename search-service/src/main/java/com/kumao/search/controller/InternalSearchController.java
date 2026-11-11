package com.kumao.search.controller;

import com.kumao.api.dto.SearchDocDTO;
import com.kumao.common.result.R;
import com.kumao.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 索引增量同步：业务服务数据新增/修改/删除时自动调用
 */
@RestController
@RequestMapping("/internal/search")
@RequiredArgsConstructor
public class InternalSearchController {

    private final SearchService searchService;

    @PostMapping("/sync")
    public R<Void> sync(@RequestBody SearchDocDTO doc) {
        searchService.sync(doc);
        return R.ok();
    }

    @DeleteMapping("/sync/{type}/{bizId}")
    public R<Void> delete(@PathVariable String type, @PathVariable Long bizId) {
        searchService.delete(type.toUpperCase(), bizId);
        return R.ok();
    }

    @PostMapping("/sync/batch")
    public R<Void> batch(@RequestBody java.util.List<SearchDocDTO> docs) {
        searchService.saveAll(docs);
        return R.ok();
    }
}
