package ru.practicum.explore.stats.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explore.dto.EndpointHitDto;
import ru.practicum.explore.dto.ViewStatsDto;
import ru.practicum.explore.stats.service.HitsService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class StatsController {

    private final HitsService hitService;

    // POST /hit — сохранение хита
    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@RequestBody EndpointHitDto dto) {

        hitService.saveHit(dto);
    }

    // GET /stats — получение статистики
    @GetMapping("/stats")
    public Collection<ViewStatsDto> getStats(
            @RequestParam("start")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,

            @RequestParam("end")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,

            @RequestParam(name = "uris", required = false) List<String> uris,

            @RequestParam(name = "unique", defaultValue = "false") boolean unique
    ) {

        if (uris == null || uris.isEmpty()) {
            uris = Collections.emptyList();
        }

        return hitService.findStats(start, end, uris, unique);
    }
}