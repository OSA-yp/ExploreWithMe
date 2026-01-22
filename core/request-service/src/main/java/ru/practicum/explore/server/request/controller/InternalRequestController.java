package ru.practicum.explore.server.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.explore.server.request.service.ParticipationRequestService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {
    private final ParticipationRequestService participationRequestService;

    @GetMapping("/confirmed-count")
    public Map<Long, Long> confirmedCount(@RequestParam(name = "eventIds") List<Long> eventIds) {
        return participationRequestService.getConfirmedCounts(eventIds);
    }
}

