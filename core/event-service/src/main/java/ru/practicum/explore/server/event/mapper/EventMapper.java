package ru.practicum.explore.server.event.mapper;

import ru.practicum.explore.server.category.dto.CategoryResponseDto;
import ru.practicum.explore.server.event.dto.EventFullDto;
import ru.practicum.explore.server.event.dto.EventShortDto;
import ru.practicum.explore.server.event.dto.NewEventDto;
import ru.practicum.explore.server.event.enums.EventState;
import ru.practicum.explore.server.event.model.Event;
import ru.practicum.explore.server.users.dto.UserShortDto;

import java.time.LocalDateTime;

public class EventMapper {

    public static Event toEvent(NewEventDto dto, Long initiatorId) {
        if (dto == null) {
            return null;
        }

        return Event.builder()
                .annotation(dto.getAnnotation())
                .categoryId(dto.getCategory())
                .description(dto.getDescription())
                .eventDate(dto.getEventDate())
                .location(dto.getLocation())
                .paid(Boolean.TRUE.equals(dto.getPaid()))
                .participantLimit(dto.getParticipantLimit())
                .requestModeration(dto.getRequestModeration())
                .title(dto.getTitle())
                .initiatorId(initiatorId)
                .createdOn(LocalDateTime.now())
                .state(EventState.PENDING)
                .build();
    }

    public static EventShortDto toEventShortDto(Event event,
                                               CategoryResponseDto category,
                                               UserShortDto initiator,
                                               long confirmedRequests,
                                               long views) {
        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(category);
        dto.setEventDate(event.getEventDate());
        dto.setInitiator(initiator);
        dto.setPaid(event.isPaid());
        dto.setTitle(event.getTitle());
        dto.setConfirmedRequests(confirmedRequests);
        dto.setViews(views);
        return dto;
    }

    public static EventFullDto toEventFullDto(Event event,
                                             CategoryResponseDto category,
                                             UserShortDto initiator,
                                             long confirmedRequests,
                                             long views) {
        EventFullDto dto = new EventFullDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(category);
        dto.setEventDate(event.getEventDate());
        dto.setInitiator(initiator);
        dto.setPaid(event.isPaid());
        dto.setTitle(event.getTitle());
        dto.setConfirmedRequests(confirmedRequests);
        dto.setViews(views);

        dto.setCreatedOn(event.getCreatedOn());
        dto.setDescription(event.getDescription());
        dto.setLocation(event.getLocation());
        dto.setParticipantLimit(event.getParticipantLimit() == null ? 0 : event.getParticipantLimit());
        dto.setRequestModeration(event.getRequestModeration());
        dto.setState(event.getState());
        dto.setPublishedOn(event.getPublishedOn());
        return dto;
    }
}

