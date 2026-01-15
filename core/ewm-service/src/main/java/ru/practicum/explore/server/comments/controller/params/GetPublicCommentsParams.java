package ru.practicum.explore.server.comments.controller.params;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetPublicCommentsParams {

    public long eventId;

    public int from;

    public int size;
}
