package ru.practicum.explore.server.comments.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explore.server.comments.controller.params.CommentStatusAction;
import ru.practicum.explore.server.comments.controller.params.GetAdminCommentsFilter;
import ru.practicum.explore.server.comments.controller.params.GetAdminCommentsParams;
import ru.practicum.explore.server.comments.dto.FullCommentResponseDto;
import ru.practicum.explore.server.comments.service.CommentsService;
import ru.practicum.explore.server.exception.ValidationException;

import java.util.Collection;

@Slf4j
@RestController
@AllArgsConstructor
public class AdminCommentsController {

    private final CommentsService commentsService;

    @GetMapping("/admin/comments")
    @ResponseStatus(HttpStatus.OK)
    public Collection<FullCommentResponseDto> geComments(
            @Valid
            @RequestParam(name = "filter", defaultValue = "NEW", required = false)
            GetAdminCommentsFilter filter,
            @Valid
            @RequestParam(name = "from", defaultValue = "0", required = false)
            Integer from,
            @Valid
            @RequestParam(name = "size", defaultValue = "10", required = false)
            Integer size) {
        log.info("Запрос событий администратором: from={}, size={}, filter={}", from, size, filter);
        if (size < 1) {
            throw new ValidationException("Size parameter must be >=1");
        }
        if (from < 0) {
            throw new ValidationException("From parameter must be >=0");
        }
        GetAdminCommentsParams params = new GetAdminCommentsParams();
        params.setFilter(filter);
        params.setFrom(from);
        params.setSize(size);
        return commentsService.getAdminComments(params);
    }

    @PatchMapping("/admin/comments/{commentId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void setApprovedOrRejectComment(
            @PathVariable
            Long commentId,
            @RequestParam(name = "status")
            CommentStatusAction newStatus
    ) {
        commentsService.approveOrRejectComment(commentId, newStatus);
    }

    @DeleteMapping("/admin/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable
            Long commentId) {
        commentsService.adminDeleteComment(commentId);
    }
}
