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
import ru.practicum.explore.server.event.dto.EventFullDto;
import ru.practicum.explore.server.event.service.PublicEventService;
import ru.practicum.explore.server.exception.ForbiddenException;
import ru.practicum.explore.server.exception.NotFoundException;
import ru.practicum.explore.server.users.service.UserService;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class CommentsServiceImpl implements CommentsService {

    private final CommentsRepository commentsRepository;
    private final PublicEventService publicEventService;
    private final UserService userService;


    public Collection<PublicCommentResponseDto> getPublicEventComments(GetPublicCommentsParams params) {

        checkEvent(params.eventId);
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
        checkEvent(params.getEventId(), params.getUserId());

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

        comment.setText(params.getDto().getText());
        comment.setStatus(CommentStatus.NEW);

        return CommentMapper.toFullCommentResponseDto(commentsRepository.save(comment));
    }

    @Override
    public void deleteComment(DeleteCommentParams params) {

        Comment comment = checkAndGetComment(params.getCommentId());

        checkUserForHimSelf(comment.getCommentator(), params.getUserId(),
                "Удалять можно только свои комментарии");

        commentsRepository.delete(comment);
    }

    public Collection<FullCommentResponseDto> getAdminComments(GetAdminCommentsParams params) {

        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());

        CommentStatus status = null; // для фильтра all

        switch (params.getFilter()) {
            case NEW -> status = CommentStatus.NEW;
            case PUBLISHED -> status = CommentStatus.PUBLISHED;
            case REJECTED -> status = CommentStatus.REJECTED;
        }

        Page<Comment> comments;

        if (status != null) {
            comments = commentsRepository.findAllByStatusOrderByCreatedAsc(status, pageable);
        } else {
            comments = commentsRepository.findAllByOrderByCreatedAsc(pageable);
        }

        return comments.stream()
                .map(CommentMapper::toFullCommentResponseDto)
                .toList();
    }

    @Override
    public void approveOrRejectComment(Long commentId, CommentStatusAction newStatus) {

        Comment comment = checkAndGetComment(commentId);

        if (comment.getStatus() != CommentStatus.NEW) {
            throw new ForbiddenException("Can't change status for comment with id="
                    + commentId + " because it's current status in not NEW");
        }

        switch (newStatus) {
            case APPROVED -> comment.setStatus(CommentStatus.PUBLISHED);
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
            comments = commentsRepository.findAllByCommentator(params.getUserId(), pageable);
        } else {
            comments = commentsRepository.findAllByCommentatorAndStatus(params.getUserId(), status, pageable);
        }
        return comments.stream()
                .map(CommentMapper::toFullCommentResponseDto)
                .collect(Collectors.toList());
    }

    private CommentStatus parseFilter(String filter) {
        return switch (filter.toUpperCase()) {
            case "NEW" -> CommentStatus.NEW;
            case "PUBLISHED" -> CommentStatus.PUBLISHED;
            case "REJECTED" -> CommentStatus.REJECTED;
            case "ALL" -> null;
            default -> throw new NotFoundException("Некорректный фильтр по статусу: " + filter);
        };
    }

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

        // проверка на существование пользователя на стороне сервиса пользователей
        userService.checkUser(userId);
    }

    private EventFullDto checkEvent(Long eventId) {

        // проверка на существование события на стороне сервиса событий
        // так как получаются только опубликованные события, то для всех остальных будет 404
        return publicEventService.getPublicEventById(eventId);
    }

    private void checkEvent(Long eventId, Long userId) {

        EventFullDto event = checkEvent(eventId);

        if (Objects.equals(userId, event.getInitiator().getId())) {
            throw new ForbiddenException("Комментировать можно только чужие события");
        }
    }

    private void checkUserForHimSelf(Long user1Id, Long user2Id, String errorText) {

        if (!user1Id.equals(user2Id)) {
            throw new ForbiddenException(errorText);
        }
    }
}
