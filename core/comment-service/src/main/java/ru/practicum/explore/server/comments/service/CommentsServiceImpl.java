package ru.practicum.explore.server.comments.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.explore.server.comments.controller.params.*;
import ru.practicum.explore.server.comments.dal.CommentMapper;
import ru.practicum.explore.server.comments.dal.CommentsRepository;
import ru.practicum.explore.server.comments.dto.FullCommentResponseDto;
import ru.practicum.explore.server.comments.dto.PublicCommentResponseDto;
import ru.practicum.explore.server.comments.model.Comment;
import ru.practicum.explore.server.comments.model.CommentStatus;
import ru.practicum.explore.server.event.client.EventInternalClient;
import ru.practicum.explore.server.event.dto.InternalEventDto;
import ru.practicum.explore.server.event.enums.EventState;
import ru.practicum.explore.server.exception.ForbiddenException;
import ru.practicum.explore.server.exception.NotFoundException;
import ru.practicum.explore.server.users.client.UserInternalClient;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class CommentsServiceImpl implements CommentsService {

    private final CommentsRepository commentsRepository;
    private final EventInternalClient eventInternalClient;
    private final UserInternalClient userInternalClient;

    @Override
    public Collection<PublicCommentResponseDto> getPublicEventComments(GetPublicCommentsParams params) {
        checkPublishedEvent(params.eventId);
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());
        Page<Comment> comments = commentsRepository.findByEventAndStatusOrderByPublishedDesc(
                params.eventId,
                CommentStatus.PUBLISHED,
                pageable);
        return comments.stream()
                .map(CommentMapper::toPublicCommentResponseDto)
                .toList();
    }

    @Override
    public FullCommentResponseDto addComment(AddCommentParams params) {
        checkUser(params.getUserId());
        checkEventForComment(params.getEventId(), params.getUserId());

        Comment comment = CommentMapper.toComment(params);
        Comment newComment = commentsRepository.save(comment);
        log.info("Comment with id={} was created", newComment.getId());
        return CommentMapper.toFullCommentResponseDto(newComment);
    }

    @Override
    public FullCommentResponseDto updateComment(UpdateCommentParams params) {
        Comment comment = checkAndGetComment(params.getCommentId());

        checkUserForHimSelf(comment.getCommentator(), params.getUserId(),
                "Редактировать можно только свои комментарии");

        comment.setText(params.getDto().getText().trim());
        comment.setStatus(CommentStatus.NEW);
        comment.setPublished(null);

        return CommentMapper.toFullCommentResponseDto(commentsRepository.save(comment));
    }

    @Override
    public void deleteComment(DeleteCommentParams params) {
        Comment comment = checkAndGetComment(params.getCommentId());
        checkUserForHimSelf(comment.getCommentator(), params.getUserId(),
                "Удалять можно только свои комментарии");
        commentsRepository.delete(comment);
    }

    @Override
    public Collection<FullCommentResponseDto> getAdminComments(GetAdminCommentsParams params) {
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());

        CommentStatus status = null; // для фильтра ALL
        switch (params.getFilter()) {
            case NEW -> status = CommentStatus.NEW;
            case PUBLISHED -> status = CommentStatus.PUBLISHED;
            case REJECTED -> status = CommentStatus.REJECTED;
            case ALL -> status = null;
        }

        Page<Comment> comments = status != null
                ? commentsRepository.findAllByStatusOrderByCreatedAsc(status, pageable)
                : commentsRepository.findAllByOrderByCreatedAsc(pageable);

        return comments.stream()
                .map(CommentMapper::toFullCommentResponseDto)
                .toList();
    }

    @Override
    public void approveOrRejectComment(Long commentId, CommentStatusAction newStatus) {
        Comment comment = checkAndGetComment(commentId);
        if (comment.getStatus() != CommentStatus.NEW) {
            throw new ForbiddenException("Can't change status for comment with id=" + commentId + " because it's current status in not NEW");
        }

        switch (newStatus) {
            case APPROVED -> {
                comment.setStatus(CommentStatus.PUBLISHED);
                comment.setPublished(java.time.LocalDateTime.now());
            }
            case REJECTED -> comment.setStatus(CommentStatus.REJECTED);
        }

        commentsRepository.save(comment);
        log.info("Status of comment with id={} was changed to {}", commentId, comment.getStatus());
    }

    @Override
    public List<FullCommentResponseDto> getUserComments(GetUserCommentsParams params) {
        checkUser(params.getUserId());
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());
        CommentStatus status = parseFilter(params.getFilter());
        List<Comment> comments;
        if (status == null) {
            comments = commentsRepository.findAllByCommentatorOrderByCreatedDesc(params.getUserId(), pageable);
        } else {
            comments = commentsRepository.findAllByCommentatorAndStatusOrderByCreatedDesc(params.getUserId(), status, pageable);
        }
        return comments.stream()
                .map(CommentMapper::toFullCommentResponseDto)
                .collect(Collectors.toList());
    }

    private CommentStatus parseFilter(String filter) {
        String safeFilter = (filter == null ? "ALL" : filter.trim());
        return switch (safeFilter.toUpperCase()) {
            case "NEW" -> CommentStatus.NEW;
            case "PUBLISHED" -> CommentStatus.PUBLISHED;
            case "REJECTED" -> CommentStatus.REJECTED;
            case "ALL" -> null;
            default -> throw new ru.practicum.explore.server.exception.ValidationException(
                    "Некорректный фильтр по статусу: " + filter
            );
        };
    }

    @Override
    public void adminDeleteComment(Long commentId) {
        Comment comment = checkAndGetComment(commentId);
        commentsRepository.delete(comment);
        log.info("Comment with id={} was deleted", commentId);
    }

    private Comment checkAndGetComment(Long commentId) {
        return commentsRepository.getCommentById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));
    }

    private void checkUser(Long userId) {
        try {
            userInternalClient.getUser(userId);
        } catch (Exception e) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private InternalEventDto checkPublishedEvent(Long eventId) {
        InternalEventDto event;
        try {
            event = eventInternalClient.getEvent(eventId);
        } catch (Exception e) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        return event;
    }

    private void checkEventForComment(Long eventId, Long userId) {
        InternalEventDto event = checkPublishedEvent(eventId);
        if (Objects.equals(userId, event.getInitiatorId())) {
            throw new ForbiddenException("Комментировать можно только чужие события");
        }
    }

    private void checkUserForHimSelf(Long user1Id, Long user2Id, String errorText) {
        if (!user1Id.equals(user2Id)) {
            throw new ForbiddenException(errorText);
        }
    }
}

