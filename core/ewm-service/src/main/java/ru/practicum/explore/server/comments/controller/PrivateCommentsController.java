package ru.practicum.explore.server.comments.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explore.server.comments.controller.params.AddCommentParams;
import ru.practicum.explore.server.comments.controller.params.DeleteCommentParams;
import ru.practicum.explore.server.comments.controller.params.GetUserCommentsParams;
import ru.practicum.explore.server.comments.controller.params.UpdateCommentParams;
import ru.practicum.explore.server.comments.dto.FullCommentResponseDto;
import ru.practicum.explore.server.comments.dto.RequestCommentDto;
import ru.practicum.explore.server.comments.service.CommentsService;

import java.util.List;

@RestController
@AllArgsConstructor
public class PrivateCommentsController {

    private final CommentsService commentsService;

    private static final String EWM_USER_HEADER = "X-EWM-User-Id";

    @PostMapping("/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public FullCommentResponseDto addComment(
            @Valid
            @RequestBody
            RequestCommentDto requestCommentDto,
            @PathVariable
            Long eventId,
            @RequestHeader(EWM_USER_HEADER)
            Long userId) {

        AddCommentParams params = new AddCommentParams();
        params.setRequestCommentDto(requestCommentDto);
        params.setEventId(eventId);
        params.setUserId(userId);

        return commentsService.addComment(params);
    }

    @PatchMapping("/comments/{commentId}")
    public FullCommentResponseDto updateComment(
            @RequestHeader(EWM_USER_HEADER)
            Long userId,
            @PathVariable
            Long commentId,
            @RequestBody @Valid
            RequestCommentDto updateCommentDto) {

        UpdateCommentParams params = new UpdateCommentParams();
        params.setUserId(userId);
        params.setCommentId(commentId);
        params.setDto(updateCommentDto);
        return commentsService.updateComment(params);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deleteComment(
            @RequestHeader(EWM_USER_HEADER)
            Long userId,
            @PathVariable
            Long commentId) {

        DeleteCommentParams params = new DeleteCommentParams();
        params.setUserId(userId);
        params.setCommentId(commentId);

        commentsService.deleteComment(params);
    }

    @GetMapping("/users/comments")
    public List<FullCommentResponseDto> getUserComments(
            @RequestHeader(EWM_USER_HEADER)
            Long userId,
            @RequestParam(defaultValue = "all")
            String filter,
            @RequestParam(defaultValue = "0")
            int from,
            @RequestParam(defaultValue = "10")
            int size) {

        GetUserCommentsParams params = new GetUserCommentsParams();
        params.setUserId(userId);
        params.setFilter(filter);
        params.setFrom(from);
        params.setSize(size);

        return commentsService.getUserComments(params);
    }
}
