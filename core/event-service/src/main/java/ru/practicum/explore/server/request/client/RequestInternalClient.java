package ru.practicum.explore.server.request.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service", path = "/internal/requests")
public interface RequestInternalClient {
    @GetMapping("/confirmed-count")
    Map<Long, Long> confirmedCount(@RequestParam("eventIds") List<Long> eventIds);
}

