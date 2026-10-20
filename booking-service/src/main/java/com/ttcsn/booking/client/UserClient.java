package com.ttcsn.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Gọi user-service kiểm tra người dùng tồn tại (internal).
 */
@FeignClient(name = "user-service", url = "${app.services.user-url:http://localhost:8084}")
public interface UserClient {

    @GetMapping("/internal/users/{userId}/exists")
    boolean userExists(@PathVariable("userId") Long userId);
}
