package ru.practicum.explore.server.event.service;

import ru.practicum.explore.server.event.dto.EventFullDto;
import ru.practicum.explore.server.event.dto.UpdateEventAdminRequest;
import ru.practicum.explore.server.event.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminEventService {
    List<EventFullDto> getEvents(List<Long> users, List<EventState> states, List<Long> categories,
                                 LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);

    EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest updateRequest);
}