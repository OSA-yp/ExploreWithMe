package ru.practicum.explore.server.comments.controller.params;

public enum GetAdminCommentsFilter {
    NEW,
    PUBLISHED,
    REJECTED,
    ALL;

    public static GetAdminCommentsFilter fromParam(String value) {
        if (value == null || value.isBlank()) {
            return NEW;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "NEW" -> NEW;
            case "PUBLISHED" -> PUBLISHED;
            case "REJECTED" -> REJECTED;
            case "ALL" -> ALL;
            default -> throw new ru.practicum.explore.server.exception.ValidationException(
                    "Некорректный фильтр по статусу: " + value
            );
        };
    }
}

