package ru.practicum.explore.server.comments.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import ru.practicum.explore.server.comments.model.CommentStatus;

import java.time.LocalDateTime;

@Data
public class FullCommentResponseDto {

    private Long id;

    private Long commentator;

    private Long event;

    private String text;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime created;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime published;

    private CommentStatus status;
}

