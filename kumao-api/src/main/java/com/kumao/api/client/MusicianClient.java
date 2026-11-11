package com.kumao.api.client;

import com.kumao.api.dto.MusicianDTO;
import com.kumao.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "musician-service", contextId = "musicianClient")
public interface MusicianClient {

    @GetMapping("/internal/musician/by-user/{userId}")
    R<MusicianDTO> byUser(@PathVariable("userId") Long userId);
}
