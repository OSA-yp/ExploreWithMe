package ru.practicum.explore.server.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.StatsClient;
import ru.practicum.explore.dto.ViewStatsDto;
import ru.practicum.explore.dto.ViewsStatsRequest;
import ru.practicum.explore.server.category.dal.CategoryRepository;
import ru.practicum.explore.server.category.model.Category;
import ru.practicum.explore.server.config.AppConfig;
import ru.practicum.explore.server.event.dto.EventFullDto;
import ru.practicum.explore.server.event.dto.EventShortDto;
import ru.practicum.explore.server.event.dto.NewEventDto;
import ru.practicum.explore.server.event.dto.UpdateEventUserRequest;
import ru.practicum.explore.server.event.enums.EventState;
import ru.practicum.explore.server.event.enums.UserStateAction;
import ru.practicum.explore.server.event.mapper.EventMapper;
import ru.practicum.explore.server.event.model.Event;
import ru.practicum.explore.server.event.repository.EventRepository;
import ru.practicum.explore.server.exception.ConflictException;
import ru.practicum.explore.server.exception.NotFoundException;
import ru.practicum.explore.server.exception.ValidationException;
import ru.practicum.explore.server.request.enums.RequestStatus;
import ru.practicum.explore.server.request.repository.ParticipationRequestRepository;
import ru.practicum.explore.server.users.dal.UserRepository;
import ru.practicum.explore.server.users.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static ru.practicum.explore.server.event.service.EventFullDtoCreator.feelViewsField;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivateEventServiceImpl implements PrivateEventService {

    private static final Map<UserStateAction, EventState> statusMap = Map.of(
            UserStateAction.CANCEL_REVIEW, EventState.CANCELED,
            UserStateAction.SEND_TO_REVIEW, EventState.PENDING
    );

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ParticipationRequestRepository participationRequestRepository;
    private final AppConfig config;

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден."));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с id=" + newEventDto.getCategory() + " не найдена."));

        Event event = EventMapper.toEvent(newEventDto);
        event.setInitiator(user);
        event.setCategory(category);
        event.setCreatedOn(LocalDateTime.now());

        event = eventRepository.save(event);
        log.info("Event with id={} was created", event.getId());

        return EventMapper.toEventFullDto(event);
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

        if (updateRequest.getCategory() != null) {
            var category = categoryRepository.getCategoryById(updateRequest.getCategory());
            if (category == null) {
                throw new NotFoundException("Категория не найдена");
            }
            event.setCategory(category);
        }

        updateField(updateRequest.getEventDate(), event::setEventDate);
        updateField(updateRequest.getLocation(), event::setLocation);
        updateField(updateRequest.getPaid(), event::setPaid);
        updateField(updateRequest.getParticipantLimit(), event::setParticipantLimit);

        event.setState(Optional.ofNullable(updateRequest.getStateAction())
                .map(statusMap::get)
                .orElse(EventState.PENDING));

        Event updated = eventRepository.save(event);
        EventFullDto dto = EventMapper.toEventFullDto(updated);
        dto.setConfirmedRequests(participationRequestRepository.countByEventAndStatus(eventId, RequestStatus.CONFIRMED));
        return dto;
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "createdOn"));

        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);
        if (events.isEmpty()) return List.of();

        // Подсчёт подтверждённых заявок
        Map<Long, Long> confirmedRequestsMap = getconfirmedRequestsMap(events);

        // Подготовка URI -> ID и дат
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
                .map(event -> {
                    EventShortDto dto = EventMapper.toEventShortDto(event);
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));
                    dto.setViews(viewsMap.getOrDefault("/events/" + event.getId(), 0L));
                    return dto;
                }).collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено или не принадлежит пользователю id=" + userId));
        StatsClient statsClient = new StatsClient(config.getStatsServerUrl());

        return feelViewsField(eventId, event, participationRequestRepository, statsClient);
    }

    private Event getEventByIdAndCheckUser(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено."));

        if (!event.getInitiator().getId().equals(userId)) {
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

    private Map<Long, Long> getconfirmedRequestsMap(List<Event> events) {
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> map = participationRequestRepository
                .countConfirmedRequestsForEvents(eventIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
        return map;
    }

    private <T> void updateField(T value, Consumer<T> setter) {
        if (value != null) setter.accept(value);
    }
}