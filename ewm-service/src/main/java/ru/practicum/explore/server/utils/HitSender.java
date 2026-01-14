package ru.practicum.explore.server.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.StatsClient;
import ru.practicum.explore.dto.EndpointHitDto;
import ru.practicum.explore.server.config.AppConfig;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@AllArgsConstructor
public class HitSender {
    private final AppConfig config;


    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void send(HttpServletRequest request) {
        StatsClient statsClient = new StatsClient(config.getStatsServerUrl());

        EndpointHitDto hit = new EndpointHitDto(config.getEwmServiceName(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                LocalDateTime.now().format(DATE_TIME_FORMATTER));
        statsClient.sendHit(hit);
    }
}
