package com.kumao.api.client;

import com.kumao.api.dto.AdPlanPaymentDTO;
import com.kumao.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ad-service", contextId = "adClient")
public interface AdClient {

    @GetMapping("/internal/ad/plans/{id}/payment-info")
    R<AdPlanPaymentDTO> paymentInfo(@PathVariable("id") Long id);

    @PostMapping("/internal/ad/plans/{id}/paid")
    R<Void> markPaid(@PathVariable("id") Long id, @RequestParam("orderNo") String orderNo);
}
