package ru.practicum.explore.server.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.cloud.client.discovery.DiscoveryClient;
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
    private final DiscoveryClient discoveryClient;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void send(HttpServletRequest request) {
        StatsClient statsClient = new StatsClient(discoveryClient, "stats-server");

        String clientIp = getClientIp(request);
        String uri = request.getRequestURI();
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        EndpointHitDto hit = new EndpointHitDto(config.getEwmServiceName(),
                uri,
                clientIp,
                timestamp);

        statsClient.sendHit(hit);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For может содержать несколько IP через запятую
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
