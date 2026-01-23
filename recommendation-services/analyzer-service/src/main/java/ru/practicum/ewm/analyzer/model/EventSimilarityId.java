package ru.practicum.ewm.analyzer.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EventSimilarityId implements Serializable {
    private Long eventA;
    private Long eventB;
}
