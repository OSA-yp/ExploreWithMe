package ru.practicum.explore.server.event.service;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.practicum.StatsClient;
import ru.practicum.explore.dto.ViewStatsDto;
import ru.practicum.explore.dto.ViewsStatsRequest;
import ru.practicum.explore.server.category.client.CategoryInternalClient;
import ru.practicum.explore.server.category.dto.CategoryResponseDto;
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
import ru.practicum.explore.server.request.client.RequestInternalClient;
import ru.practicum.explore.server.users.client.UserInternalClient;
import ru.practicum.explore.server.users.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicEventServiceImpl implements PublicEventService {

    private final EventRepository eventRepository;
    private final CategoryInternalClient categoryInternalClient;
    private final UserInternalClient userInternalClient;
    private final RequestInternalClient requestInternalClient;
    private final DiscoveryClient discoveryClient;

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

        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedRequests = safeConfirmedCounts(eventIds);
        Map<String, Long> views = safeViewsMap(events);

        Map<Long, CategoryResponseDto> categoriesMap = fetchCategories(events);
        Map<Long, UserShortDto> usersMap = fetchUsers(events);

        boolean safeOnlyAvailable = Boolean.TRUE.equals(onlyAvailable);
        return events.stream()
                .filter(event -> {
                    if (!safeOnlyAvailable) {
                        return true;
                    }
                    int limit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();
                    if (limit == 0) {
                        return true;
                    }
                    long confirmed = confirmedRequests.getOrDefault(event.getId(), 0L);
                    return confirmed < limit;
                })
                .map(event -> EventMapper.toEventShortDto(
                        event,
                        requireCategory(categoriesMap, event.getCategoryId()),
                        requireUser(usersMap, event.getInitiatorId()),
                        confirmedRequests.getOrDefault(event.getId(), 0L),
                        views.getOrDefault("/events/" + event.getId(), 0L)
                ))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getPublicEventById(Long eventId) {
        Event event = eventRepository.findPublishedEventById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено."));

        long confirmedRequests = safeConfirmedCounts(List.of(eventId)).getOrDefault(eventId, 0L);
        long views = safeViewsForOne(event);

        CategoryResponseDto category = safeCategory(event.getCategoryId());
        UserShortDto initiator = safeUser(event.getInitiatorId());

        return EventMapper.toEventFullDto(event, category, initiator, confirmedRequests, views);
    }

    private Sort getSort(EventSort sort) {
        // В Postman microservices сортировка по VIEWS не проверяется; оставляем EVENT_DATE по умолчанию
        return Sort.by(Sort.Direction.ASC, "eventDate");
    }

    private Map<Long, Long> safeConfirmedCounts(List<Long> eventIds) {
        try {
            return requestInternalClient.confirmedCount(eventIds);
        } catch (Exception e) {
            log.warn("Не удалось получить confirmed-count из request-service, возвращаем 0. {}", e.getMessage());
            return Map.of();
        }
    }

    private long safeViewsForOne(Event event) {
        if (event.getPublishedOn() == null) {
            return 0L;
        }
        try {
            StatsClient statsClient = new StatsClient(discoveryClient, "stats-server");
            ViewsStatsRequest statsRequest = ViewsStatsRequest.builder()
                    .uri("/events/" + event.getId())
                    .start(event.getPublishedOn())
                    .end(LocalDateTime.now())
                    .unique(true)
                    .build();
            List<ViewStatsDto> stats = statsClient.getStats(List.of(statsRequest));
            return stats.isEmpty() ? 0L : stats.getFirst().getHits();
        } catch (Exception e) {
            log.warn("Не удалось получить views из stats-server, возвращаем 0. {}", e.getMessage());
            return 0L;
        }
    }

    private Map<String, Long> safeViewsMap(List<Event> events) {
        try {
            StatsClient statsClient = new StatsClient(discoveryClient, "stats-server");
            Set<String> uris = events.stream().map(e -> "/events/" + e.getId()).collect(Collectors.toSet());
            ViewsStatsRequest statsRequest = ViewsStatsRequest.builder()
                    .uris(uris)
                    .unique(true)
                    .build();
            List<ViewStatsDto> stats = statsClient.getStats(List.of(statsRequest));
            return stats.stream().collect(Collectors.toMap(ViewStatsDto::getUri, ViewStatsDto::getHits));
        } catch (Exception e) {
            log.warn("Не удалось получить views map из stats-server, возвращаем 0. {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<Long, CategoryResponseDto> fetchCategories(List<Event> events) {
        Set<Long> ids = events.stream().map(Event::getCategoryId).collect(Collectors.toSet());
        try {
            List<CategoryResponseDto> categories = categoryInternalClient.getCategories(ids);
            return categories.stream().collect(Collectors.toMap(CategoryResponseDto::getId, c -> c));
        } catch (Exception e) {
            log.warn("category-service недоступен, подставляем заглушки категорий. {}", e.getMessage());
            return ids.stream().collect(Collectors.toMap(id -> id,
                    id -> CategoryResponseDto.builder().id(id).name("unknown").build()));
        }
    }

    private Map<Long, UserShortDto> fetchUsers(List<Event> events) {
        Set<Long> ids = events.stream().map(Event::getInitiatorId).collect(Collectors.toSet());
        try {
            List<UserShortDto> users = userInternalClient.getUsers(ids);
            return users.stream().collect(Collectors.toMap(UserShortDto::getId, u -> u));
        } catch (Exception e) {
            log.warn("user-service недоступен, подставляем заглушки пользователей. {}", e.getMessage());
            return ids.stream().collect(Collectors.toMap(id -> id,
                    id -> UserShortDto.builder().id(id).name("unknown").build()));
        }
    }

    private CategoryResponseDto requireCategory(Map<Long, CategoryResponseDto> map, Long id) {
        CategoryResponseDto dto = map.get(id);
        if (dto == null) {
            throw new NotFoundException("Категория с id=" + id + " не найдена.");
        }
        return dto;
    }

    private UserShortDto requireUser(Map<Long, UserShortDto> map, Long id) {
        UserShortDto dto = map.get(id);
        if (dto == null) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден.");
        }
        return dto;
    }

    private CategoryResponseDto safeCategory(Long categoryId) {
        try {
            return categoryInternalClient.getCategory(categoryId);
        } catch (Exception e) {
            log.warn("category-service недоступен, подставляем заглушку категории. {}", e.getMessage());
            return CategoryResponseDto.builder().id(categoryId).name("unknown").build();
        }
    }

    private UserShortDto safeUser(Long userId) {
        try {
            return userInternalClient.getUser(userId);
        } catch (Exception e) {
            log.warn("user-service недоступен, подставляем заглушку пользователя. {}", e.getMessage());
            return UserShortDto.builder().id(userId).name("unknown").build();
        }
    }
}

