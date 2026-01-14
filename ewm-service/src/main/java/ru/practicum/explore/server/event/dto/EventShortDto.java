package ru.practicum.explore.server.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;
import ru.practicum.explore.server.category.dto.CategoryResponseDto;
import ru.practicum.explore.server.users.dto.UserShortDto;

import java.time.LocalDateTime;

@Data
public class EventShortDto {

    private Long id;

    @NotBlank
    @Size(min = 20, max = 2000)
    private String annotation;

    @NotNull
    private CategoryResponseDto category;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    @NotNull
    private UserShortDto initiator;

    @NotNull
    private Boolean paid;

    @NotBlank
    private String title;

    @PositiveOrZero
    private long confirmedRequests;

    @PositiveOrZero
    private long views;
}