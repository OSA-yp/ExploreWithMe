package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.practicum.exception.StatsServiceException;
import ru.practicum.explore.dto.EndpointHitDto;
import ru.practicum.explore.dto.ViewStatsDto;
import ru.practicum.explore.dto.ViewsStatsRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class StatsClient {
    private static final String HIT_ENDPOINT = "/hit";
    private static final String STATS_ENDPOINT = "/stats";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestClient restClient;

    public StatsClient(@Value("${stats-service.url}") String statsServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(statsServiceUrl)
                .build();
    }

    public void sendHit(EndpointHitDto hit) {
        try {
            restClient.post()
                    .uri(HIT_ENDPOINT)
                    .body(hit)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Статистика успешно отправлена");
        } catch (RestClientException e) {
            log.error("Ошибка при отправке статистики: {}", e.getMessage(), e);
            throw new StatsServiceException("Ошибка при отправке статистики", e);
        }
    }

    public List<ViewStatsDto> getStats(List<ViewsStatsRequest> requests) {
        List<ViewStatsDto> allStats = new ArrayList<>();
        for (ViewsStatsRequest req : requests) {
            try {
                List<ViewStatsDto> stats = restClient.get()
                        .uri(uriBuilder -> uriBuilder.path(STATS_ENDPOINT)
                                .queryParam("start", DATE_TIME_FORMATTER.format(req.getStart()))
                                .queryParam("end", DATE_TIME_FORMATTER.format(req.getEnd()))
                                .queryParam("uris", req.getUris())
                                .queryParam("unique", req.isUnique())
                                .build())
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {
                        });
                assert stats != null;
                allStats.addAll(stats);
            } catch (RestClientException e) {
                log.error("Ошибка при запросе статистики", e);
                throw new StatsServiceException("Ошибка сервиса статистики", e);
            } catch (Exception e) {
                log.error("Ошибка при запросе статистики: {}", e.getMessage(), e);
            }
        }
        return allStats;
    }

    private void validateDateTimeParameters(String start, String end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Дата и время начала и окончания диапазона должны быть указаны");
        }

        try {
            LocalDateTime startDate = LocalDateTime.parse(start, DATE_TIME_FORMATTER);
            LocalDateTime endDate = LocalDateTime.parse(end, DATE_TIME_FORMATTER);

            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Неверный формат даты. Ожидается yyyy-MM-dd HH:mm:ss", e);
        }
    }
}