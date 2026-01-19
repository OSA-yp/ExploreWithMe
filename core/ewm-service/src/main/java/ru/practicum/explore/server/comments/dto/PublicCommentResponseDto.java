package ru.practicum.explore.server.comments.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PublicCommentResponseDto {

    private Long id;

    private Long commentator;

    private Long event;

    private String text;

    private LocalDateTime published;

}
