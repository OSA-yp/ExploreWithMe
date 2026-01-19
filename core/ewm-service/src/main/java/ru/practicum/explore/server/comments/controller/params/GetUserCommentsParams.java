package ru.practicum.explore.server.comments.controller.params;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetUserCommentsParams {
    Long userId;
    String filter;
    int from;
    int size;
}
