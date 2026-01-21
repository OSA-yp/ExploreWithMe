package ru.practicum.explore.server.compilation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "compilations")
@Getter
@Setter
@NoArgsConstructor
public class Compilation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean pinned;

    @Column(nullable = false)
    private String title;

    @ElementCollection
    @CollectionTable(
            name = "compilations_events",
            joinColumns = @JoinColumn(name = "compilation_id")
    )
    @Column(name = "event_id", nullable = false)
    private Set<Long> eventIds = new HashSet<>();
}

