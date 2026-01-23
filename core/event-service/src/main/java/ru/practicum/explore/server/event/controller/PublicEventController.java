package ru.practicum.explore.server.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explore.server.event.dto.EventFullDto;
import ru.practicum.explore.server.event.dto.EventShortDto;
import ru.practicum.explore.server.event.enums.EventSort;
import ru.practicum.explore.server.event.service.PublicEventService;
import ru.practicum.CollectorClient;
import ru.practicum.ewm.stats.proto.ActionTypeProto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final PublicEventService eventService;
    private final CollectorClient collectorClient;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<EventShortDto>> getEvents(@RequestParam(required = false, defaultValue = "") String text,
                                                         @RequestParam(required = false) List<Long> categories,
                                                         @RequestParam(required = false) Boolean paid,
                                                         @RequestParam(required = false)
                                                         @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                         LocalDateTime rangeStart,
                                                         @RequestParam(required = false)
                                                         @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                         LocalDateTime rangeEnd,
                                                         @RequestParam(required = false, defaultValue = "false")
                                                         Boolean onlyAvailable,
                                                         @RequestParam(required = false) EventSort sort,
                                                         @RequestParam(defaultValue = "0") int from,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         HttpServletRequest request) {

        log.info("Запрос публичных событий: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        // Удалена отправка информации о просмотре из GET /events
        List<EventShortDto> events = eventService.getPublicEvents(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<EventFullDto> getEvent(@PathVariable Long id, 
                                                 @RequestHeader(value = "X-EWM-USER-ID", required = false) Long userId,
                                                 HttpServletRequest request) {
        log.info("Запрос события с идентификатором {}", id);

        EventFullDto event = eventService.getPublicEventById(id);
        
        // Отправка просмотра в Collector
        if (userId != null) {
            try {
                collectorClient.collectUserAction(userId, id, ActionTypeProto.ACTION_VIEW, Instant.now());
            } catch (Exception e) {
                log.warn("Не удалось отправить просмотр в Collector: userId={}, eventId={}", userId, id, e);
                // Не ломаем основной функционал при ошибке
            }
        }

        return ResponseEntity.ok(event);
    }

    @GetMapping("/recommendations")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<EventShortDto>> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Long userId,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Запрос рекомендаций для пользователя: userId={}, size={}", userId, size);
        List<EventShortDto> recommendations = eventService.getRecommendations(userId, size);
        return ResponseEntity.ok(recommendations);
    }

    @PutMapping("/{eventId}/like")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> likeEvent(
            @PathVariable Long eventId,
            @RequestHeader("X-EWM-USER-ID") Long userId) {
        log.info("Лайк мероприятия: userId={}, eventId={}", userId, eventId);
        eventService.likeEvent(userId, eventId);
        return ResponseEntity.ok().build();
    }
}

