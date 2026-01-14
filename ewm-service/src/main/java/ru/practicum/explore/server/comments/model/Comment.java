package ru.practicum.explore.server.comments.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "commentator_id", nullable = false)
    private Long commentator;

    @Column(name = "event_id", nullable = false)
    private Long event;

    @Column(name = "text", nullable = false, length = 2000)
    private String text;

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Column(name = "published", nullable = false)
    private LocalDateTime published;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private CommentStatus status;
}
