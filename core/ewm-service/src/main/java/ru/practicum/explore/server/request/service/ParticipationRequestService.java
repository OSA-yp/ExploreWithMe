package ru.practicum.explore.server.request.service;

import ru.practicum.explore.server.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.explore.server.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.explore.server.request.dto.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {
    ParticipationRequestDto createRequest(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest request);

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);
}