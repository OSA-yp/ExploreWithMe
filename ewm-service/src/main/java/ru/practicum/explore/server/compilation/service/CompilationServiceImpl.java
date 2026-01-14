package ru.practicum.explore.server.compilation.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.explore.server.compilation.dto.CompilationDto;
import ru.practicum.explore.server.compilation.dto.NewCompilationDto;
import ru.practicum.explore.server.compilation.dto.UpdateCompilationRequest;
import ru.practicum.explore.server.compilation.mapper.CompilationMapper;
import ru.practicum.explore.server.compilation.model.Compilation;
import ru.practicum.explore.server.compilation.repository.CompilationRepository;
import ru.practicum.explore.server.event.model.Event;
import ru.practicum.explore.server.event.repository.EventRepository;
import ru.practicum.explore.server.exception.ConflictException;
import ru.practicum.explore.server.exception.NotFoundException;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CompilationDto saveCompilation(NewCompilationDto newCompilationDto) {
        if (compilationRepository.existsByTitle(newCompilationDto.getTitle())) {
            throw new ConflictException("Подборка с названием ='" + newCompilationDto.getTitle() + "' уже существует.");
        }

        // Загружаем события по ID
        Set<Event> events = newCompilationDto.getEvents() == null
                ? Collections.emptySet()
                : new HashSet<>(eventRepository.findAllById(newCompilationDto.getEvents()));

        Compilation compilation = CompilationMapper.toEntity(newCompilationDto);
        compilation.setEvents(events);

        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Сохранили подборку: {}.", savedCompilation);
        return CompilationMapper.toDto(savedCompilation);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Compilation> compilations;

        if (pinned == null) {
            compilations = compilationRepository.findAll(pageable);
        } else {
            compilations = compilationRepository.findByPinned(pinned, pageable);
        }

        return compilations.stream()
                .map(CompilationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = findCompilationById(compId);
        log.info("Возвращаем подборку: {}.", compilation);
        return CompilationMapper.toDto(compilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id=" + compId + " не найдена.");
        }
        compilationRepository.deleteById(compId);
        log.info("Подборка с id={} удалена.", compId);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        Compilation compilation = findCompilationById(compId);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            compilation.setTitle(request.getTitle());
        }
        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }
        if (request.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(request.getEvents()));
            compilation.setEvents(events);
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Подборка с id={} обновлена.", compId);
        return CompilationMapper.toDto(updatedCompilation);
    }

    private Compilation findCompilationById(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена."));
    }
}