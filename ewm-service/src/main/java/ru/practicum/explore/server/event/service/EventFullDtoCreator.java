package ru.practicum.explore.server.event.service;

import ru.practicum.StatsClient;
import ru.practicum.explore.dto.ViewStatsDto;
import ru.practicum.explore.dto.ViewsStatsRequest;
import ru.practicum.explore.server.event.dto.EventFullDto;
import ru.practicum.explore.server.event.mapper.EventMapper;
import ru.practicum.explore.server.event.model.Event;
import ru.practicum.explore.server.request.enums.RequestStatus;
import ru.practicum.explore.server.request.repository.ParticipationRequestRepository;

import java.util.List;

public class EventFullDtoCreator {

    static EventFullDto feelViewsField(Long eventId, Event event, ParticipationRequestRepository requestRepository, StatsClient statsClient) {
        EventFullDto dto = EventMapper.toEventFullDto(event);
        dto.setConfirmedRequests(requestRepository.countByEventAndStatus(eventId, RequestStatus.CONFIRMED));

        ViewsStatsRequest statsRequest = ViewsStatsRequest.builder()
                .uri("/events/" + eventId)
                .unique(true)
                .build();

        List<ViewStatsDto> stats = statsClient.getStats(List.of(statsRequest));
        dto.setViews(stats.isEmpty() ? 0L : stats.getFirst().getHits());

        return dto;
    }
}