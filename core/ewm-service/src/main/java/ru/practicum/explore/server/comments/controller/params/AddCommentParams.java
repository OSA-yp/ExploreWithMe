package ru.practicum.explore.server.comments.controller.params;

import lombok.Getter;
import lombok.Setter;
import ru.practicum.explore.server.comments.dto.RequestCommentDto;

@Setter
@Getter
public class AddCommentParams {

    private Long userId;

    private Long eventId;

    private RequestCommentDto requestCommentDto;

}
