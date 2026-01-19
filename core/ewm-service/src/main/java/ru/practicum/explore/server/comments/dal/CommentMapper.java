package ru.practicum.explore.server.comments.dal;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.explore.server.comments.controller.params.AddCommentParams;
import ru.practicum.explore.server.comments.dto.FullCommentResponseDto;
import ru.practicum.explore.server.comments.dto.PublicCommentResponseDto;
import ru.practicum.explore.server.comments.model.Comment;
import ru.practicum.explore.server.comments.model.CommentStatus;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class CommentMapper {

    public static FullCommentResponseDto toFullCommentResponseDto(Comment comment) {


        FullCommentResponseDto dto = new FullCommentResponseDto();

        dto.setId(comment.getId());
        dto.setCommentator(comment.getCommentator());
        dto.setEvent(comment.getEvent());
        dto.setText(comment.getText());
        dto.setCreated(comment.getCreated());
        dto.setStatus(comment.getStatus());

        if (comment.getPublished() != null) {
            dto.setPublished(comment.getPublished());
        }

        return dto;
    }

    public static PublicCommentResponseDto toPublicCommentResponseDto(Comment comment) {

        PublicCommentResponseDto dto = new PublicCommentResponseDto();

        dto.setId(comment.getId());
        dto.setCommentator(comment.getCommentator());
        dto.setEvent(comment.getEvent());
        dto.setText(comment.getText());
        if (comment.getPublished() != null) {
            dto.setPublished(comment.getPublished());
        }

        return dto;
    }

    public static Comment toComment(AddCommentParams params) {

        Comment comment = new Comment();

        comment.setCommentator(params.getUserId());
        comment.setEvent(params.getEventId());
        comment.setText(params.getRequestCommentDto().getText());
        comment.setCreated(LocalDateTime.now());
        comment.setStatus(CommentStatus.NEW);

        return comment;

    }

}
