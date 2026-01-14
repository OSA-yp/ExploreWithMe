package ru.practicum.explore.server.event.service;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.practicum.StatsClient;
import ru.practicum.explore.dto.ViewStatsDto;
import ru.practicum.explore.dto.ViewsStatsRequest;
import ru.practicum.explore.server.config.AppConfig;
import ru.practicum.explore.server.event.dto.EventFullDto;
import ru.practicum.explore.server.event.dto.EventShortDto;
import ru.practicum.explore.server.event.enums.EventSort;
import ru.practicum.explore.server.event.enums.EventState;
import ru.practicum.explore.server.event.mapper.EventMapper;
import ru.practicum.explore.server.event.model.Event;
import ru.practicum.explore.server.event.repository.EventRepository;
import ru.practicum.explore.server.exception.AppException;
import ru.practicum.explore.server.exception.NotFoundException;
import ru.practicum.explore.server.exception.ValidationException;
import ru.practicum.explore.server.request.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.practicum.explore.server.event.service.EventFullDtoCreator.feelViewsField;


@Slf4j
@Service
@RequiredArgsConstructor
public class PublicEventServiceImpl implements PublicEventService {

    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final AppConfig config;

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, EventSort sort, int from, int size) {

        if (from < 0 || size <= 0) {
            throw new AppException("Ошибка: некорректные параметры пагинации", HttpStatus.BAD_REQUEST);
        }

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Ошибка: начальная дата не может быть позже конечной.");
        }

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }

        Pageable pageable = PageRequest.of(from / size, size, getSort(sort));

        List<Event> events = eventRepository.findWithFilters(null, List.of(EventState.PUBLISHED),
                categories, rangeStart, rangeEnd, paid,
                StringUtils.isNotBlank(text) ? text : null,
                pageable);

        log.info("Найдено событий: {}", events.size());
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedRequests = requestRepository.countConfirmedRequestsForEvents(eventIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        Map<String, Event> uriToEventMap = events.stream()
                .collect(Collectors.toMap(e -> "/events/" + e.getId(), e -> e));

        // Запрос статистики
        ViewsStatsRequest statsRequest = ViewsStatsRequest.builder()
                .uris(uriToEventMap.keySet())
                .unique(true)
                .build();
        StatsClient statsClient = new StatsClient(config.getStatsServerUrl());

        List<ViewStatsDto> stats = statsClient.getStats(List.of(statsRequest));

        Map<String, Long> viewsMap = stats.stream()
                .collect(Collectors.toMap(ViewStatsDto::getUri, ViewStatsDto::getHits));

        return events.stream()
                .filter(event -> !onlyAvailable || confirmedRequests.getOrDefault(event.getId(), 0L) < event.getParticipantLimit())
                .map(event -> {
                    EventShortDto dto = eventMapper.toEventShortDto(event);
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L));
                    dto.setViews(viewsMap.getOrDefault("/events/" + event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getPublicEventById(Long eventId) {
        Event event = eventRepository.findPublishedEventById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено."));
        StatsClient statsClient = new StatsClient(config.getStatsServerUrl());

        return feelViewsField(eventId, event, requestRepository, statsClient);
    }

    private Sort getSort(EventSort sort) {
        return sort == EventSort.VIEWS
                ? Sort.by(Sort.Direction.DESC, "views") :
                Sort.by(Sort.Direction.ASC, "eventDate");
    }
}