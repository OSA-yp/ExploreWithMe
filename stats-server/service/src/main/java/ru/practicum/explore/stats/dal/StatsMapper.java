package ru.practicum.explore.stats.dal;

import org.springframework.stereotype.Component;
import ru.practicum.explore.dto.ViewStatsDto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StatsMapper {

    public List<ViewStatsDto> toViewStats(List<Map<String, Object>> results) {
        return results.stream()
                .map(this::toViewStatsDto)
                .collect(Collectors.toList());
    }

    private ViewStatsDto toViewStatsDto(Map<String, Object> row) {
        return new ViewStatsDto(
                (String) row.get("app"),
                (String) row.get("uri"),
                ((Number) row.get("hits")).longValue()
        );
    }
}