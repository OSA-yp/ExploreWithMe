package ru.practicum.explore.server.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class AppConfig {
    @Value("${ewm.ewm-service.name}")
    private String ewmServiceName;
}

