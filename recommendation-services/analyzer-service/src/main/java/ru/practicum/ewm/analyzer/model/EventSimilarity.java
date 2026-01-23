package ru.practicum.ewm.analyzer.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_similarities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(EventSimilarityId.class)
public class EventSimilarity {

    @Id
    @Column(name = "event_a")
    private Long eventA;

    @Id
    @Column(name = "event_b")
    private Long eventB;

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
