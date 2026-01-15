package ru.practicum.explore.server.comments.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explore.server.comments.controller.params.GetPublicCommentsParams;
import ru.practicum.explore.server.comments.dto.PublicCommentResponseDto;
import ru.practicum.explore.server.comments.service.CommentsService;
import ru.practicum.explore.server.exception.ValidationException;

import java.util.Collection;

@Slf4j
@RestController
@AllArgsConstructor
public class PublicCommentsController {

    private final CommentsService commentsService;

    @GetMapping("/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.OK)
    public Collection<PublicCommentResponseDto> getEventComments(
            @PathVariable
            Long eventId,
            @Valid
            @RequestParam(name = "from", defaultValue = "0", required = false)
            Integer from,
            @Valid
            @RequestParam(name = "size", defaultValue = "10", required = false)
            Integer size) {

        log.info("Запрос публичных событий: eventId={}, from={}, size={}", eventId, from, size);

        if (size < 1) {
            throw new ValidationException("Size parameter must be >=1");
        }
        if (from < 0) {
            throw new ValidationException("From parameter must be >=0");
        }
        GetPublicCommentsParams params = new GetPublicCommentsParams();
        params.setEventId(eventId);
        params.setFrom(from);
        params.setSize(size);

        return commentsService.getPublicEventComments(params);
    }


}
