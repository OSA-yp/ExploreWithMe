package ru.practicum.explore.server.event.service;

import ru.practicum.explore.server.event.dto.EventFullDto;
import ru.practicum.explore.server.event.dto.EventShortDto;
import ru.practicum.explore.server.event.enums.EventSort;

import java.time.LocalDateTime;
import java.util.List;

public interface PublicEventService {
    List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                        Boolean onlyAvailable, EventSort sort, int from, int size);

    EventFullDto getPublicEventById(Long eventId);
}