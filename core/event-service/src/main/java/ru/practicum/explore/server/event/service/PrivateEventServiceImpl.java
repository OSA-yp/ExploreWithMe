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
import ru.practicum.explore.server.event.dto.EventShortDto;
import ru.practicum.explore.server.event.dto.NewEventDto;
import ru.practicum.explore.server.event.dto.UpdateEventUserRequest;
import ru.practicum.explore.server.event.enums.EventState;
import ru.practicum.explore.server.event.enums.UserStateAction;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivateEventServiceImpl implements PrivateEventService {

    private static final Map<UserStateAction, EventState> STATUS_MAP = Map.of(
            UserStateAction.CANCEL_REVIEW, EventState.CANCELED,
            UserStateAction.SEND_TO_REVIEW, EventState.PENDING
    );

    private final EventRepository eventRepository;
    private final CategoryInternalClient categoryInternalClient;
    private final UserInternalClient userInternalClient;
    private final RequestInternalClient requestInternalClient;
    private final AnalyzerClient analyzerClient;

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        UserShortDto initiator;
        CategoryResponseDto category;
        try {
            initiator = userInternalClient.getUser(userId);
        } catch (Exception e) {
            throw new AppException("Сервис пользователей недоступен", HttpStatus.SERVICE_UNAVAILABLE);
        }
        try {
            category = categoryInternalClient.getCategory(newEventDto.getCategory());
        } catch (Exception e) {
            throw new AppException("Сервис категорий недоступен", HttpStatus.SERVICE_UNAVAILABLE);
        }

        Event event = EventMapper.toEvent(newEventDto, userId);
        event.setState(EventState.PENDING);

        Event saved = eventRepository.save(event);
        log.info("Event with id={} was created", saved.getId());

        return EventMapper.toEventFullDto(saved, category, initiator, 0L, 0.0);
    }

    @Override
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        Event event = getEventByIdAndCheckUser(eventId, userId);

        if (updateRequest.getEventDate() != null &&
                updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата и время события не могут быть раньше, чем через 2 часа от текущего момента.");
        }

        updateField(updateRequest.getTitle(), event::setTitle);
        updateField(updateRequest.getAnnotation(), event::setAnnotation);
        updateField(updateRequest.getDescription(), event::setDescription);
        updateField(updateRequest.getEventDate(), event::setEventDate);
        updateField(updateRequest.getLocation(), event::setLocation);
        updateField(updateRequest.getPaid(), paid -> event.setPaid(Boolean.TRUE.equals(paid)));
        updateField(updateRequest.getParticipantLimit(), event::setParticipantLimit);

        if (updateRequest.getCategory() != null) {
            try {
                categoryInternalClient.getCategory(updateRequest.getCategory());
                event.setCategoryId(updateRequest.getCategory());
            } catch (Exception e) {
                throw new AppException("Сервис категорий недоступен", HttpStatus.SERVICE_UNAVAILABLE);
            }
        }

        if (updateRequest.getStateAction() != null) {
            event.setState(Optional.ofNullable(STATUS_MAP.get(updateRequest.getStateAction()))
                    .orElse(event.getState()));
        }

        Event updated = eventRepository.save(event);

        long confirmed = safeConfirmed(updated.getId());
        Double rating = safeRating(updated.getId());
        CategoryResponseDto category = safeCategory(updated.getCategoryId());
        UserShortDto initiator = safeUser(updated.getInitiatorId());

        return EventMapper.toEventFullDto(updated, category, initiator, confirmed, rating);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        if (from < 0 || size <= 0) {
            throw new AppException("Ошибка: некорректные параметры пагинации", HttpStatus.BAD_REQUEST);
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "createdOn"));
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmed = safeConfirmedCounts(eventIds);
        Map<Long, Double> ratings = safeRatingsMap(eventIds);

        Set<Long> categoryIds = events.stream().map(Event::getCategoryId).collect(Collectors.toSet());
        Map<Long, CategoryResponseDto> categories;
        try {
            categories = categoryInternalClient.getCategories(categoryIds).stream()
                    .collect(Collectors.toMap(CategoryResponseDto::getId, c -> c));
        } catch (Exception e) {
            categories = categoryIds.stream().collect(Collectors.toMap(id -> id,
                    id -> CategoryResponseDto.builder().id(id).name("unknown").build()));
        }

        UserShortDto initiator = safeUser(userId);
        final Map<Long, CategoryResponseDto> categoriesFinal = categories;
        final Map<Long, Long> confirmedFinal = confirmed;
        final Map<Long, Double> ratingsFinal = ratings;

        return events.stream()
                .map(e -> EventMapper.toEventShortDto(
                        e,
                        categoriesFinal.get(e.getCategoryId()),
                        initiator,
                        confirmedFinal.getOrDefault(e.getId(), 0L),
                        ratingsFinal.getOrDefault(e.getId(), 0.0)
                ))
                .toList();
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Событие с id=" + eventId + " не найдено или не принадлежит пользователю id=" + userId));

        long confirmed = safeConfirmed(eventId);
        Double rating = safeRating(eventId);
        CategoryResponseDto category = safeCategory(event.getCategoryId());
        UserShortDto initiator = safeUser(event.getInitiatorId());

        return EventMapper.toEventFullDto(event, category, initiator, confirmed, rating);
    }

    private Event getEventByIdAndCheckUser(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено."));

        if (!event.getInitiatorId().equals(userId)) {
            throw new ValidationException("Изменять событие может только его инициатор.");
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Опубликованное событие нельзя редактировать.");
        }

        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ValidationException("Изменять можно только отменённые события или события в ожидании публикации.");
        }

        return event;
    }

    private <T> void updateField(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private long safeConfirmed(Long eventId) {
        try {
            return requestInternalClient.confirmedCount(List.of(eventId)).getOrDefault(eventId, 0L);
        } catch (Exception e) {
            return 0L;
        }
    }

    private Map<Long, Long> safeConfirmedCounts(List<Long> eventIds) {
        try {
            return requestInternalClient.confirmedCount(eventIds);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Double safeRating(Long eventId) {
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

