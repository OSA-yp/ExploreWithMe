package ru.practicum.explore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ViewStatsDto {
    private String app;
    private String uri;
    private Long hits;
}