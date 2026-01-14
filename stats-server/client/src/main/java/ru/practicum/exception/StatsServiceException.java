package ru.practicum.exception;

public class StatsServiceException extends RuntimeException {
    public StatsServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}