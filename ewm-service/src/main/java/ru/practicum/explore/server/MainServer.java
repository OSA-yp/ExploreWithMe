package ru.practicum.explore.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
@ConfigurationPropertiesScan
public class MainServer {
    public static void main(String[] args) {
        SpringApplication.run(MainServer.class, args);
    }
}
