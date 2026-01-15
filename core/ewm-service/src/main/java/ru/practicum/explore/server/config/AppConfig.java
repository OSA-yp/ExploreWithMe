package ru.practicum.explore.server.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties("ewm")
@Component
public class AppConfig {

    @Value("${ewm.ewm-service.name}")
    private String ewmServiceName;

    @Value("${ewm.stats-server.url}")
    private String statsServerUrl;
}
