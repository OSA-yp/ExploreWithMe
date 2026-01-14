package ru.practicum.explore.stats.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;


@Slf4j
@Getter
public class ErrorResponse {
    private final String error;
    private final String description;

    public ErrorResponse(String error, String description, StackTraceElement[] stack) {
        this.error = error;
        this.description = description;

        log.warn("{}   ---   {}   ---   {}", error, description, Arrays.stream(stack).findFirst());
    }
}