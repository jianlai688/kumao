package com.kumao.api.client;

import com.kumao.api.dto.SearchDocDTO;
import com.kumao.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "search-service", contextId = "searchClient")
public interface SearchClient {

    @PostMapping("/internal/search/sync")
    R<Void> sync(@RequestBody SearchDocDTO doc);

    @DeleteMapping("/internal/search/sync/{type}/{bizId}")
    R<Void> delete(@PathVariable("type") String type, @PathVariable("bizId") Long bizId);
}
