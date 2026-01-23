package ru.practicum.ewm.analyzer.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_event_interactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserEventInteractionId.class)
public class UserEventInteraction {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "max_weight", nullable = false)
    private Double maxWeight;

    @Column(name = "last_action_timestamp", nullable = false)
    private LocalDateTime lastActionTimestamp;
}
