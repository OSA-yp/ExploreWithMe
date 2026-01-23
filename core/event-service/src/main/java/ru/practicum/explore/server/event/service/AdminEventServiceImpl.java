package ru.practicum.explore.server.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.practicum.AnalyzerClient;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.explore.server.category.client.CategoryInternalClient;
import ru.practicum.explore.server.category.dto.CategoryResponseDto;
import ru.practicum.explore.server.event.dto.EventFullDto;
import ru.practicum.explore.server.event.dto.UpdateEventAdminRequest;
import ru.practicum.explore.server.event.enums.EventState;
import ru.practicum.explore.server.event.enums.StateAction;
import ru.practicum.explore.server.event.mapper.EventMapper;
import ru.practicum.explore.server.event.model.Event;
import ru.practicum.explore.server.event.repository.EventRepository;
import ru.practicum.explore.server.exception.AppException;
import ru.practicum.explore.server.exception.ConflictException;
import ru.practicum.explore.server.exception.NotFoundException;
import ru.practicum.explore.server.exception.ValidationException;
import ru.practicum.explore.server.request.client.RequestInternalClient;
import ru.practicum.explore.server.users.client.UserInternalClient;
import ru.practicum.explore.server.users.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEventServiceImpl implements AdminEventService {

    private final EventRepository eventRepository;
    private final CategoryInternalClient categoryInternalClient;
    private final UserInternalClient userInternalClient;
    private final RequestInternalClient requestInternalClient;
    private final AnalyzerClient analyzerClient;

    @Override
    public List<EventFullDto> getEvents(List<Long> users, List<EventState> states, List<Long> categories,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        if (from < 0 || size <= 0) {
            throw new AppException("Ошибка: некорректные параметры пагинации", HttpStatus.BAD_REQUEST);
        }
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Ошибка: начальная дата не может быть позже конечной.");
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "createdOn"));
        List<Event> events = eventRepository.findWithFilters(users, states, categories, rangeStart, rangeEnd, null, null, pageable);
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmed = safeConfirmedCounts(eventIds);
        Map<Long, Double> ratings = safeRatingsMap(eventIds);

        Map<Long, CategoryResponseDto> categoriesMap;
        Set<Long> categoryIds = events.stream().map(Event::getCategoryId).collect(Collectors.toSet());
        try {
            categoriesMap = categoryInternalClient
                    .getCategories(categoryIds)
                    .stream()
                    .collect(Collectors.toMap(CategoryResponseDto::getId, c -> c));
        } catch (Exception e) {
            categoriesMap = categoryIds.stream().collect(Collectors.toMap(id -> id,
                    id -> CategoryResponseDto.builder().id(id).name("unknown").build()));
        }

        Map<Long, UserShortDto> usersMap;
        Set<Long> userIds = events.stream().map(Event::getInitiatorId).collect(Collectors.toSet());
        try {
            usersMap = userInternalClient
                    .getUsers(userIds)
                    .stream()
                    .collect(Collectors.toMap(UserShortDto::getId, u -> u));
        } catch (Exception e) {
            usersMap = userIds.stream().collect(Collectors.toMap(id -> id,
                    id -> UserShortDto.builder().id(id).name("unknown").build()));
        }

        final Map<Long, CategoryResponseDto> categoriesFinal = categoriesMap;
        final Map<Long, UserShortDto> usersFinal = usersMap;
        final Map<Long, Long> confirmedFinal = confirmed;
        final Map<Long, Double> ratingsFinal = ratings;

        return events.stream()
                .map(e -> EventMapper.toEventFullDto(
                        e,
                        categoriesFinal.get(e.getCategoryId()),
                        usersFinal.get(e.getInitiatorId()),
                        confirmedFinal.getOrDefault(e.getId(), 0L),
                        ratingsFinal.getOrDefault(e.getId(), 0.0)
                ))
                .toList();
    }

    @Override
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено."));

        if (updateRequest.getEventDate() != null) {
            // Точечная валидация: нельзя установить дату события в прошлом (ожидается 400).
            // Для сценария публикации правила проверяются ниже и должны возвращать 409.
            if (updateRequest.getStateAction() != StateAction.PUBLISH_EVENT
                    && updateRequest.getEventDate().isBefore(LocalDateTime.now())) {
                throw new ValidationException("Дата начала события не может быть в прошлом.");
            }
            event.setEventDate(updateRequest.getEventDate());
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getLocation() != null) {
            event.setLocation(updateRequest.getLocation());
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(Boolean.TRUE.equals(updateRequest.getPaid()));
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getCategory() != null) {
            try {
                categoryInternalClient.getCategory(updateRequest.getCategory());
                event.setCategoryId(updateRequest.getCategory());
            } catch (Exception e) {
                throw new AppException("Сервис категорий недоступен", HttpStatus.SERVICE_UNAVAILABLE);
            }
        }

        if (updateRequest.getStateAction() != null) {
            if (updateRequest.getStateAction() == StateAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие можно публиковать, только если оно в состоянии ожидания публикации");
                }
                if (event.getEventDate() != null && event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                    throw new ConflictException("Дата начала события должна быть не ранее чем за час от даты публикации");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            }
            if (updateRequest.getStateAction() == StateAction.REJECT_EVENT) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Событие можно отклонить, только если оно еще не опубликовано");
                }
                event.setState(EventState.CANCELED);
            }
        }

        Event saved = eventRepository.save(event);

        long confirmed = safeConfirmedCounts(List.of(eventId)).getOrDefault(eventId, 0L);
        Double rating = safeRatingForOne(saved.getId());
        CategoryResponseDto category = safeCategory(saved.getCategoryId());
        UserShortDto initiator = safeUser(saved.getInitiatorId());
        return EventMapper.toEventFullDto(saved, category, initiator, confirmed, rating);
    }

    private Map<Long, Long> safeConfirmedCounts(List<Long> eventIds) {
        try {
            return requestInternalClient.confirmedCount(eventIds);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<Long, Double> safeRatingsMap(List<Long> eventIds) {
        try {
            return analyzerClient.getInteractionsCount(eventIds)
                    .collect(Collectors.toMap(
                            RecommendedEventProto::getEventId,
                            RecommendedEventProto::getScore
                    ));
        } catch (Exception e) {
            log.warn("Не удалось получить ratings map из analyzer, возвращаем 0.0. {}", e.getMessage());
            return Map.of();
        }
    }

    private Double safeRatingForOne(Long eventId) {
        try {
            return analyzerClient.getInteractionsCount(List.of(eventId))
                    .findFirst()
                    .map(RecommendedEventProto::getScore)
                    .orElse(0.0);
        } catch (Exception e) {
            log.warn("Не удалось получить rating из analyzer, возвращаем 0.0. {}", e.getMessage());
            return 0.0;
        }
    }

    private CategoryResponseDto safeCategory(Long categoryId) {
        try {
            return categoryInternalClient.getCategory(categoryId);
        } catch (Exception e) {
            return CategoryResponseDto.builder().id(categoryId).name("unknown").build();
        }
    }

    private UserShortDto safeUser(Long userId) {
        try {
            return userInternalClient.getUser(userId);
        } catch (Exception e) {
            return UserShortDto.builder().id(userId).name("unknown").build();
        }
    }
}

