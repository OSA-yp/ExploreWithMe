package ru.practicum.explore.server.event.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.explore.server.event.dto.EventShortDto;

import java.util.Collection;
import java.util.List;

@FeignClient(name = "event-service", path = "/internal/events")
public interface EventInternalClient {
    @GetMapping("/short")
    List<EventShortDto> getEventsShort(@RequestParam("ids") Collection<Long> ids);
}

