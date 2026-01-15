package ru.practicum.explore.server.event.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.explore.server.category.dal.CategoryMapper;
import ru.practicum.explore.server.category.model.Category;
import ru.practicum.explore.server.event.dto.EventFullDto;
import ru.practicum.explore.server.event.dto.EventShortDto;
import ru.practicum.explore.server.event.dto.NewEventDto;
import ru.practicum.explore.server.event.enums.EventState;
import ru.practicum.explore.server.event.model.Event;
import ru.practicum.explore.server.users.dal.UserMapper;

@Component
public class EventMapper {


    public static EventShortDto toEventShortDto(Event event) {
        EventShortDto dto = new EventShortDto();

        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(CategoryMapper.toDto(event.getCategory()));
        dto.setEventDate(event.getEventDate());
        dto.setInitiator(UserMapper.toUserShortDto(event.getInitiator()));
        dto.setPaid(event.isPaid());
        dto.setTitle(event.getTitle());


        return dto;
    }

    public static EventFullDto toEventFullDto(Event event) {

        EventFullDto dto = new EventFullDto();

        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(CategoryMapper.toDto(event.getCategory()));
        dto.setEventDate(event.getEventDate());
        dto.setInitiator(UserMapper.toUserShortDto(event.getInitiator()));
        dto.setPaid(event.isPaid());
        dto.setTitle(event.getTitle());

        dto.setCreatedOn(event.getCreatedOn());
        dto.setDescription(event.getDescription());
        dto.setLocation(event.getLocation());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setRequestModeration(event.getRequestModeration());
        dto.setState(event.getState());

        return dto;
    }

    public static Event toEvent(NewEventDto dto) {
        if (dto == null) {
            return null;
        }

        Event event = new Event();
        event.setAnnotation(dto.getAnnotation());
        event.setCategory(mapCategory(dto.getCategory()));
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setLocation(dto.getLocation());
        event.setPaid(dto.getPaid());
        event.setParticipantLimit(dto.getParticipantLimit());
        event.setRequestModeration(dto.getRequestModeration());
        event.setTitle(dto.getTitle());
        event.setState(EventState.PENDING);

        return event;
    }


    private static Category mapCategory(Long id) {
        return id == null ? null : Category.builder().id(id).build();
    }
}