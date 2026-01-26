package ru.practicum.explore.server.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.explore.server.category.client.CategoryInternalClient;
import ru.practicum.explore.server.category.dto.CategoryResponseDto;
import ru.practicum.explore.server.event.dto.EventShortDto;
import ru.practicum.explore.server.event.dto.InternalEventDto;
import ru.practicum.explore.server.event.mapper.EventMapper;
import ru.practicum.explore.server.event.model.Event;
import ru.practicum.explore.server.event.repository.EventRepository;
import ru.practicum.explore.server.exception.NotFoundException;
import ru.practicum.explore.server.request.client.RequestInternalClient;
import ru.practicum.explore.server.users.client.UserInternalClient;
import ru.practicum.explore.server.users.dto.UserShortDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventsController {
    private final EventRepository eventRepository;
    private final CategoryInternalClient categoryInternalClient;
    private final UserInternalClient userInternalClient;
    private final RequestInternalClient requestInternalClient;

    @GetMapping("/{eventId}")
    public InternalEventDto getEvent(@PathVariable Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено."));
        InternalEventDto dto = new InternalEventDto();
        dto.setId(event.getId());
        dto.setInitiatorId(event.getInitiatorId());
        dto.setCategoryId(event.getCategoryId());
        dto.setState(event.getState());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setRequestModeration(event.getRequestModeration());
        return dto;
    }

    @GetMapping("/exists-by-category")
    public boolean existsByCategory(@RequestParam("categoryId") Long categoryId) {
        return eventRepository.existsByCategoryId(categoryId);
    }

    @GetMapping("/short")
    public List<EventShortDto> getEventsShort(@RequestParam("ids") Collection<Long> ids) {
        List<Event> events = eventRepository.findAllById(ids);
        Map<Long, Event> byId = events.stream().collect(Collectors.toMap(Event::getId, e -> e));

        Set<Long> categoryIds = events.stream().map(Event::getCategoryId).collect(Collectors.toSet());
        Map<Long, CategoryResponseDto> categories;
        try {
            categories = categoryInternalClient.getCategories(categoryIds).stream()
                    .collect(Collectors.toMap(CategoryResponseDto::getId, c -> c));
        } catch (Exception e) {
            categories = categoryIds.stream().collect(Collectors.toMap(id -> id,
                    id -> CategoryResponseDto.builder().id(id).name("unknown").build()));
        }

        Set<Long> userIds = events.stream().map(Event::getInitiatorId).collect(Collectors.toSet());
        Map<Long, UserShortDto> users;
        try {
            users = userInternalClient.getUsers(userIds).stream()
                    .collect(Collectors.toMap(UserShortDto::getId, u -> u));
        } catch (Exception e) {
            users = userIds.stream().collect(Collectors.toMap(id -> id,
                    id -> UserShortDto.builder().id(id).name("unknown").build()));
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmed = safeConfirmedCounts(eventIds);
        final Map<Long, CategoryResponseDto> categoriesFinal = categories;
        final Map<Long, UserShortDto> usersFinal = users;
        final Map<Long, Long> confirmedFinal = confirmed;

        // views для internal выдачи подборок не критичны — оставляем 0, чтобы не зависеть от stats-server
        return ids.stream()
                .map(id -> {
                    Event e = byId.get(id);
                    if (e == null) {
                        throw new NotFoundException("Событие с id=" + id + " не найдено.");
                    }
                    return EventMapper.toEventShortDto(
                            e,
                            categoriesFinal.get(e.getCategoryId()),
                            usersFinal.get(e.getInitiatorId()),
                            confirmedFinal.getOrDefault(id, 0L),
                            0.0
                    );
                })
                .toList();
    }

    private Map<Long, Long> safeConfirmedCounts(List<Long> eventIds) {
        try {
            return requestInternalClient.confirmedCount(eventIds);
        } catch (Exception e) {
            log.warn("Не удалось получить confirmed-count для internal выдачи: {}", e.getMessage());
            return Map.of();
        }
    }
}

