package ru.practicum.explore.server.event.service;

import ru.practicum.explore.server.event.dto.EventFullDto;
import ru.practicum.explore.server.event.dto.EventShortDto;
import ru.practicum.explore.server.event.dto.NewEventDto;
import ru.practicum.explore.server.event.dto.UpdateEventUserRequest;

import java.util.List;

public interface PrivateEventService {
    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest);

    List<EventShortDto> getUserEvents(Long userId, int from, int size);

    EventFullDto getUserEventById(Long userId, Long eventId);
}