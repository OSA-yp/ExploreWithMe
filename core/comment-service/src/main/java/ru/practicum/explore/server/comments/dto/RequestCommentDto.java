package ru.practicum.explore.server.comments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RequestCommentDto {
    @NotBlank
    @Size(min = 1, max = 2000)
    private String text;
}

