package com.kumao.api.client;

import com.kumao.api.dto.ModerationRequestDTO;
import com.kumao.api.dto.ModerationResultDTO;
import com.kumao.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "review-service", contextId = "reviewClient")
public interface ReviewClient {

    /** 评论 / 弹幕等实时内容的 AI 审核，供内部服务调用。 */
    @PostMapping("/internal/ai/moderate")
    R<ModerationResultDTO> moderate(@RequestBody ModerationRequestDTO request);
}
