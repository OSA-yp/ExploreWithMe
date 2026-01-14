package ru.practicum.explore.server.compilation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.practicum.explore.server.event.dto.EventShortDto;

import java.util.Set;

@Data
public class CompilationDto {

    private Long id;

    @NotBlank
    @Size(min = 1, max = 255)
    private String title;

    @NotNull
    private Boolean pinned;

    private Set<EventShortDto> events;
}