package com.kumao.review.controller;

import com.kumao.api.dto.ModerationRequestDTO;
import com.kumao.api.dto.ModerationResultDTO;
import com.kumao.common.result.R;
import com.kumao.review.dto.AiReviewRequest;
import com.kumao.review.dto.AiReviewResult;
import com.kumao.review.service.AiReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 供 comment-service / live-service 在用户提交内容时同步调用的内容审核接口。
 * 返回 PASS 时业务侧自动通过；REVIEW / REJECT 由业务侧保存为待管理员复核。
 */
@RestController
@RequestMapping("/internal/ai")
@RequiredArgsConstructor
public class InternalReviewController {

    private final AiReviewService aiReviewService;

    @PostMapping("/moderate")
    public R<ModerationResultDTO> moderate(@RequestBody ModerationRequestDTO request) {
        AiReviewRequest review = new AiReviewRequest();
        review.setCategory(request.getCategory());
        review.setItemId(request.getItemId());
        review.setTitle(request.getTitle());
        review.setContent(request.getContent());
        review.setMetadata(request.getMetadata());

        AiReviewResult result = aiReviewService.review(review);
        ModerationResultDTO dto = new ModerationResultDTO();
        dto.setDecision(result.getDecision());
        dto.setRiskScore(result.getRiskScore());
        dto.setReasons(result.getReasons());
        dto.setSignals(result.getSignals());
        dto.setSource(result.getSource());
        dto.setProvider(result.getProvider());
        dto.setModel(result.getModel());
        return R.ok(dto);
    }
}
