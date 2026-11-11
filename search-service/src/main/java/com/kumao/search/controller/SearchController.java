package com.kumao.search.controller;

import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.search.dto.SearchResultVO;
import com.kumao.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 全文检索：type 可为空（综合）或 SONG/SINGER/PLAYLIST/LIVE_ROOM/MUSICIAN
     */
    @GetMapping
    public R<SearchResultVO> search(@RequestParam String keyword,
                                    @RequestParam(required = false) String type,
                                    @RequestParam(defaultValue = "1") int pageNum,
                                    @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(searchService.search(keyword, type, pageNum, pageSize));
    }

    @GetMapping("/hot-keywords")
    public R<java.util.List<String>> hotKeywords() {
        return R.ok(java.util.List.of("周杰伦", "晴天", "原创", "弹唱", "电台", "翻唱"));
    }

    // ---------------- 后台 ----------------

    @GetMapping("/admin/monitor")
    public R<Map<String, Object>> monitor() {
        UserContext.requireAdmin();
        return R.ok(searchService.monitor());
    }

    @PostMapping("/admin/sync")
    public R<Void> manualSync(@RequestBody com.kumao.search.dto.ManualSyncRequest req) {
        UserContext.requireAdmin();
        searchService.saveAll(req.getDocs());
        return R.ok();
    }
}
