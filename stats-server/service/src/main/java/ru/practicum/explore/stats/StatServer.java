package ru.practicum.explore.stats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class StatServer {
    public static void main(String[] args) {
        SpringApplication.run(StatServer.class, args);
    }
}
