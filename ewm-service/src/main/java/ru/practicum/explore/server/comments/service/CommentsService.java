package ru.practicum.explore.server.comments.service;


import jakarta.transaction.Transactional;
import ru.practicum.explore.server.comments.controller.params.*;
import ru.practicum.explore.server.comments.dto.FullCommentResponseDto;
import ru.practicum.explore.server.comments.dto.PublicCommentResponseDto;

import java.util.Collection;
import java.util.List;

public interface CommentsService {

    Collection<PublicCommentResponseDto> getPublicEventComments(GetPublicCommentsParams params);

    FullCommentResponseDto addComment(AddCommentParams params);

    FullCommentResponseDto updateComment(UpdateCommentParams params);

    @Transactional
    void deleteComment(DeleteCommentParams params);

    List<FullCommentResponseDto> getUserComments(GetUserCommentsParams params);

    Collection<FullCommentResponseDto> getAdminComments(GetAdminCommentsParams params);

    void approveOrRejectComment(Long commentId, CommentStatusAction newStatus);

    void adminDeleteComment(Long commentId);

}
