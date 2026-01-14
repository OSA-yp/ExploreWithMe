package ru.practicum.explore.server.comments.controller.params;

import lombok.Getter;
import lombok.Setter;
import ru.practicum.explore.server.comments.dto.RequestCommentDto;


@Getter
@Setter
public class UpdateCommentParams {

    private Long userId;

    private Long commentId;

    private RequestCommentDto dto;
}
