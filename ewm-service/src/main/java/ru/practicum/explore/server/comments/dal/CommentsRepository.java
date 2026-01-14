package ru.practicum.explore.server.comments.dal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.explore.server.comments.model.Comment;
import ru.practicum.explore.server.comments.model.CommentStatus;
import java.util.List;
import java.util.Optional;


@Repository
public interface CommentsRepository extends JpaRepository<Comment, Long> {


    Page<Comment> findByEventAndStatusOrderByPublishedDesc(Long eventId, CommentStatus status, Pageable pageable);

    List<Comment> findAllByCommentator(Long userId, Pageable pageable);

    List<Comment> findAllByCommentatorAndStatus(Long userId, CommentStatus status, Pageable pageable);

    Page<Comment> findAllByStatusOrderByCreatedAsc(CommentStatus status, Pageable pageable);

    Page<Comment> findAllByOrderByCreatedAsc(Pageable pageable);

    Optional<Comment> getCommentById(Long id);

}
