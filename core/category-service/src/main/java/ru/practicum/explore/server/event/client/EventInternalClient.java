package ru.practicum.explore.server.event.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "event-service", path = "/internal/events")
public interface EventInternalClient {
    @GetMapping("/exists-by-category")
    boolean existsByCategory(@RequestParam("categoryId") Long categoryId);
}

