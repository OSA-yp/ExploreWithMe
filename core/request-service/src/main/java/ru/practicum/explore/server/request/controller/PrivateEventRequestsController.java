package ru.practicum.explore.server.request.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explore.server.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.explore.server.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.explore.server.request.dto.ParticipationRequestDto;
import ru.practicum.explore.server.request.service.ParticipationRequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
public class PrivateEventRequestsController {
    private final ParticipationRequestService participationRequestService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getEventRequests(@PathVariable Long userId, @PathVariable Long eventId) {
        log.info("Получен запрос на получение заявок на участие в событии id={} инициатором id={}", eventId, userId);
        return participationRequestService.getEventRequests(userId, eventId);
    }

    @PatchMapping
    public EventRequestStatusUpdateResult updateRequestStatus(@PathVariable Long userId,
                                                              @PathVariable Long eventId,
                                                              @Valid @RequestBody EventRequestStatusUpdateRequest request) {
        log.info("Получен запрос на обновление статуса заявок для eventId={}, инициатор={} : {}", eventId, userId, request);
        return participationRequestService.updateRequestStatus(userId, eventId, request);
    }
}

