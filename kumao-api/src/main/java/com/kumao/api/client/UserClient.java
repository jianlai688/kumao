package com.kumao.api.client;

import com.kumao.api.dto.UserDTO;
import com.kumao.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", contextId = "userClient")
public interface UserClient {

    @GetMapping("/internal/users/{id}")
    R<UserDTO> getUser(@PathVariable("id") Long id);

    @PostMapping("/internal/users/{id}/roles")
    R<Void> grantRole(@PathVariable("id") Long id, @RequestParam("role") String role);

    @DeleteMapping("/internal/users/{id}/roles")
    R<Void> revokeRole(@PathVariable("id") Long id, @RequestParam("role") String role);

    @GetMapping("/internal/users/{id}/muted")
    R<Boolean> isMuted(@PathVariable("id") Long id);

    @PostMapping("/internal/users/{id}/mute")
    R<Void> mute(@PathVariable("id") Long id, @RequestParam("minutes") int minutes);
}
