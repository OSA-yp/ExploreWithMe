package ru.practicum.explore.server.comments.dto;

import lombok.Data;
import ru.practicum.explore.server.comments.model.CommentStatus;

import java.time.LocalDateTime;

@Data
public class FullCommentResponseDto {

    private Long id;

    private Long commentator;

    private Long event;

    private String text;

    private LocalDateTime created;

    private LocalDateTime published;

    private CommentStatus status;
}
