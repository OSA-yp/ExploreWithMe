package ru.practicum.explore.server.comments.controller.params;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetAdminCommentsParams {

    private GetAdminCommentsFilter filter;

    private Integer from;

    private Integer size;

}
