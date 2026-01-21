package ru.practicum.explore.server.event.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.explore.server.event.dto.InternalEventDto;

@FeignClient(name = "event-service", path = "/internal/events")
public interface EventInternalClient {
    @GetMapping("/{eventId}")
    InternalEventDto getEvent(@PathVariable("eventId") Long eventId);
}

