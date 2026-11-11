package com.kumao.review.controller;

import com.kumao.common.dto.PageQuery;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.review.entity.AiReviewTask;
import com.kumao.review.service.AutoReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 自动审核状态、任务与手动触发接口，供管理后台使用。
 */
@RestController
@RequestMapping("/ai/auto")
@RequiredArgsConstructor
public class AutoReviewController {

    private final AutoReviewService autoReviewService;

    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        UserContext.requireAdmin();
        return R.ok(autoReviewService.status());
    }

    @GetMapping("/tasks")
    public R<PageResult<AiReviewTask>> tasks(PageQuery query,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String category) {
        UserContext.requireAdmin();
        return R.ok(autoReviewService.tasks(query, status, category));
    }

    @PostMapping("/run")
    public R<Map<String, Object>> run() {
        UserContext.requireAdmin();
        return R.ok(autoReviewService.trigger());
    }

    @DeleteMapping("/tasks/{category}/{itemId}")
    public R<Integer> resolve(@PathVariable String category, @PathVariable String itemId) {
        UserContext.requireAdmin();
        return R.ok(autoReviewService.resolve(category, itemId));
    }
}
