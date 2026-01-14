package ru.practicum.explore.stats.service;

import ru.practicum.explore.dto.EndpointHitDto;
import ru.practicum.explore.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface HitsService {

    void saveHit(EndpointHitDto dto);

    Collection<ViewStatsDto> findStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
