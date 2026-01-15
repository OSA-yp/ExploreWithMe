package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.practicum.exception.StatsServerUnavailable;
import ru.practicum.explore.dto.EndpointHitDto;
import ru.practicum.explore.dto.ViewStatsDto;
import ru.practicum.explore.dto.ViewsStatsRequest;

import java.net.URI;
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
    private final DiscoveryClient discoveryClient;
    private final String statsServiceId;
    private final RetryTemplate retryTemplate;

    public StatsClient(DiscoveryClient discoveryClient,
                       @Value("${spring.application.name:stats-server}") String statsServiceId) {
        this.discoveryClient = discoveryClient;
        this.statsServiceId = statsServiceId;
        this.restClient = RestClient.builder().build();
        
        // Настройка RetryTemplate согласно ТЗ
        this.retryTemplate = new RetryTemplate();
        
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        
        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
    }

    private ServiceInstance getInstance() {
        try {
            return discoveryClient
                    .getInstances(statsServiceId)
                    .getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
                    exception
            );
        }
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(context -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    public void sendHit(EndpointHitDto hit) {
        try {
            URI uri = makeUri(HIT_ENDPOINT);
            restClient.post()
                    .uri(uri)
                    .body(hit)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Статистика успешно отправлена");
        } catch (RestClientException e) {
            log.error("Ошибка при отправке статистики: {}", e.getMessage(), e);
            throw new ru.practicum.exception.StatsServiceException("Ошибка при отправке статистики", e);
        }
    }

    public List<ViewStatsDto> getStats(List<ViewsStatsRequest> requests) {
        List<ViewStatsDto> allStats = new ArrayList<>();
        for (ViewsStatsRequest req : requests) {
            try {
                URI baseUri = makeUri(STATS_ENDPOINT);
                String queryString = "start=" + DATE_TIME_FORMATTER.format(req.getStart()) +
                        "&end=" + DATE_TIME_FORMATTER.format(req.getEnd()) +
                        "&uris=" + String.join(",", req.getUris()) +
                        "&unique=" + req.isUnique();
                URI fullUri = URI.create(baseUri.toString() + "?" + queryString);
                List<ViewStatsDto> stats = restClient.get()
                        .uri(fullUri)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {
                        });
                assert stats != null;
                allStats.addAll(stats);
            } catch (RestClientException e) {
                log.error("Ошибка при запросе статистики", e);
                throw new ru.practicum.exception.StatsServiceException("Ошибка сервиса статистики", e);
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
