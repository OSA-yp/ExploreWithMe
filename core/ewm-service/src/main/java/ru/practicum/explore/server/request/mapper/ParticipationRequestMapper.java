package ru.practicum.explore.server.request.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.explore.server.request.dto.ParticipationRequestDto;
import ru.practicum.explore.server.request.model.ParticipationRequest;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ParticipationRequestMapper {

    public static ParticipationRequest toEntity(ParticipationRequestDto dto) {
        if (dto == null) {
            return null;
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setId(dto.getId());
        request.setEvent(dto.getEvent());
        request.setRequester(dto.getRequester());
        request.setStatus(dto.getStatus());
        request.setCreated(dto.getCreated());
        return request;
    }

    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        if (request == null) {
            return null;
        }

        ParticipationRequestDto dto = new ParticipationRequestDto();
        dto.setId(request.getId());
        dto.setEvent(request.getEvent());
        dto.setRequester(request.getRequester());
        dto.setStatus(request.getStatus());
        dto.setCreated(request.getCreated());
        return dto;
    }

    public static List<ParticipationRequestDto> toDtoList(List<ParticipationRequest> requests) {
        if (requests == null) {
            return null;
        }

        return requests.stream()
                .map(ParticipationRequestMapper::toDto)
                .collect(Collectors.toList());
    }
}