package ru.practicum.explore.stats.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.explore.dto.EndpointHitDto;
import ru.practicum.explore.dto.ViewStatsDto;
import ru.practicum.explore.stats.dal.HitMapper;
import ru.practicum.explore.stats.dal.HitRepository;
import ru.practicum.explore.stats.dal.StatsMapper;
import ru.practicum.explore.stats.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@AllArgsConstructor
public class HitsServiceImpl implements HitsService {

    private final HitRepository hitRepository;
    private final StatsMapper statsMapper;

    @Override
    public void saveHit(EndpointHitDto dto) {
        hitRepository.save(HitMapper.toHit(dto));
    }

    @Override
    public Collection<ViewStatsDto> findStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {

        if (end.isBefore(start)) {
            throw new ValidationException("End time must be after start time");
        }


        if (unique) {

            if (uris.isEmpty()) {
                return statsMapper.toViewStats(hitRepository.findUniqueStatsByRangeWithoutUris(start, end));
            } else {
                return statsMapper.toViewStats(hitRepository.findUniqueStatsByRangeAndUris(start, end, uris));
            }


        } else {
            if (uris.isEmpty()) {
                return statsMapper.toViewStats(hitRepository.findStatsByRangeWithoutUris(start, end));
            } else {
                return statsMapper.toViewStats(hitRepository.findStatsByRangeAndUris(start, end, uris));
            }

        }
    }
}
