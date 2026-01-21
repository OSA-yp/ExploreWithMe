package ru.practicum.explore.server.comments.controller.params;

public enum CommentStatusAction {
    APPROVED,
    REJECTED;

    public static CommentStatusAction fromParam(String value) {
        if (value == null || value.isBlank()) {
            throw new ru.practicum.explore.server.exception.ValidationException("Параметр status обязателен");
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "APPROVED" -> APPROVED;
            case "REJECTED" -> REJECTED;
            default -> throw new ru.practicum.explore.server.exception.ValidationException(
                    "Некорректный статус: " + value + ". Ожидается approved или rejected"
            );
        };
    }
}

