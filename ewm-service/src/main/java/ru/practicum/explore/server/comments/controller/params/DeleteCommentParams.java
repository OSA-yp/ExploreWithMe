package ru.practicum.explore.server.comments.controller.params;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteCommentParams {
    Long userId;
    Long commentId;
}
