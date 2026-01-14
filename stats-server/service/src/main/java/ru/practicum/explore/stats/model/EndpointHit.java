package ru.practicum.explore.stats.model;


import jakarta.persistence.*;
import lombok.*;
import ru.practicum.explore.dto.ViewStatsDto;

import java.time.LocalDateTime;

@Entity
@Table(name = "hits")
@SqlResultSetMapping(
        name = "ViewStatsDtoMapping",
        classes = @ConstructorResult(
                targetClass = ViewStatsDto.class,
                columns = {
                        @ColumnResult(name = "app", type = String.class),
                        @ColumnResult(name = "uri", type = String.class),
                        @ColumnResult(name = "hits", type = Long.class)
                }
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "app", nullable = false)
    private String app;

    @Column(name = "uri", nullable = false)
    private String uri;

    @Column(name = "ip", nullable = false)
    private String ip;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}