package ru.practicum.explore.stats.dal;


import org.springframework.stereotype.Component;
import ru.practicum.explore.dto.EndpointHitDto;
import ru.practicum.explore.stats.model.EndpointHit;

import java.time.LocalDateTime;

@Component
public class HitMapper {
    public static EndpointHit toHit(EndpointHitDto dto) {
        return new EndpointHit(
                null,
                dto.getApp(),
                dto.getUri(),
                dto.getIp(),
                LocalDateTime.parse(dto.getTimestamp(), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }
}