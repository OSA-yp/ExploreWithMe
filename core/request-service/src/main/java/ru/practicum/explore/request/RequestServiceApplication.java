package ru.practicum.explore.request;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "ru.practicum.explore")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "ru.practicum.explore.server")
@EntityScan(basePackages = "ru.practicum.explore.server")
@EnableJpaRepositories(basePackages = "ru.practicum.explore.server")
public class RequestServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RequestServiceApplication.class, args);
    }
}

