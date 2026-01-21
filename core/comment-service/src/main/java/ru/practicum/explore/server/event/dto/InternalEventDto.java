package ru.practicum.explore.server.event.dto;

import lombok.Data;
import ru.practicum.explore.server.event.enums.EventState;

@Data
public class InternalEventDto {
    private Long id;
    private Long initiatorId;
    private Long categoryId;
    private EventState state;
    private Integer participantLimit;
    private Boolean requestModeration;
}

