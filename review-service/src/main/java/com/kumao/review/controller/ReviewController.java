package com.kumao.review.controller;

import com.kumao.common.dto.PageQuery;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.review.dto.AiReviewRequest;
import com.kumao.review.dto.AiReviewResult;
import com.kumao.review.dto.ReviewLogRequest;
import com.kumao.review.entity.ReviewLog;
import com.kumao.review.service.AiModelConfigService;
import com.kumao.review.service.AiReviewService;
import com.kumao.review.service.ReviewLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ReviewController {

    private final AiReviewService aiReviewService;
    private final ReviewLogService reviewLogService;
    private final AiModelConfigService modelConfigService;

    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        UserContext.requireAdmin();
        return R.ok(modelConfigService.status());
    }

    @PostMapping("/review")
    public R<AiReviewResult> review(@Valid @RequestBody AiReviewRequest request) {
        UserContext.requireAdmin();
        return R.ok(aiReviewService.review(request));
    }

    @PostMapping("/log")
    public R<Long> log(@Valid @RequestBody ReviewLogRequest request) {
        UserContext.requireAdmin();
        return R.ok(reviewLogService.record(request));
    }

    @GetMapping("/logs")
    public R<PageResult<ReviewLog>> logs(PageQuery query,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) String decision) {
        UserContext.requireAdmin();
        return R.ok(reviewLogService.page(query, category, decision));
    }

    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        UserContext.requireAdmin();
        return R.ok(reviewLogService.statistics());
    }
}
