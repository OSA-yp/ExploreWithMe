package ru.practicum.explore.server.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.practicum.explore.server.compilation.dto.CompilationDto;
import ru.practicum.explore.server.compilation.dto.NewCompilationDto;
import ru.practicum.explore.server.compilation.dto.UpdateCompilationRequest;
import ru.practicum.explore.server.compilation.model.Compilation;
import ru.practicum.explore.server.compilation.repository.CompilationRepository;
import ru.practicum.explore.server.event.client.EventInternalClient;
import ru.practicum.explore.server.event.dto.EventShortDto;
import ru.practicum.explore.server.exception.AppException;
import ru.practicum.explore.server.exception.ConflictException;
import ru.practicum.explore.server.exception.NotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventInternalClient eventInternalClient;

    @Override
    public CompilationDto saveCompilation(NewCompilationDto newCompilationDto) {
        if (compilationRepository.existsByTitle(newCompilationDto.getTitle())) {
            throw new ConflictException("Подборка с таким названием уже существует");
        }

        Compilation compilation = new Compilation();
        compilation.setTitle(newCompilationDto.getTitle());
        compilation.setPinned(Boolean.TRUE.equals(newCompilationDto.getPinned()));

        Set<Long> eventIds = newCompilationDto.getEvents() == null ? Set.of() : new HashSet<>(newCompilationDto.getEvents());
        if (!eventIds.isEmpty()) {
            // провалидировать что события существуют
            try {
                eventInternalClient.getEventsShort(eventIds);
            } catch (Exception e) {
                throw new AppException("Сервис событий недоступен", HttpStatus.SERVICE_UNAVAILABLE);
            }
        }
        compilation.setEventIds(eventIds);

        Compilation saved = compilationRepository.save(compilation);
        return toDto(saved);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        if (from < 0 || size <= 0) {
            throw new AppException("Ошибка: некорректные параметры пагинации", HttpStatus.BAD_REQUEST);
        }
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Compilation> page = pinned == null
                ? compilationRepository.findAll(pageable)
                : compilationRepository.findByPinned(pinned, pageable);
        return page.stream().map(this::toDto).toList();
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));
        return toDto(compilation);
    }

    @Override
    public void deleteCompilation(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id=" + compId + " не найдена");
        }
        compilationRepository.deleteById(compId);
    }

    @Override
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));

        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }
        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }
        if (request.getEvents() != null) {
            Set<Long> eventIds = new HashSet<>(request.getEvents());
            if (!eventIds.isEmpty()) {
                try {
                    eventInternalClient.getEventsShort(eventIds);
                } catch (Exception e) {
                    throw new AppException("Сервис событий недоступен", HttpStatus.SERVICE_UNAVAILABLE);
                }
            }
            compilation.setEventIds(eventIds);
        }

        Compilation saved = compilationRepository.save(compilation);
        return toDto(saved);
    }

    private CompilationDto toDto(Compilation compilation) {
        CompilationDto dto = new CompilationDto();
        dto.setId(compilation.getId());
        dto.setTitle(compilation.getTitle());
        dto.setPinned(compilation.isPinned());

        Set<Long> ids = compilation.getEventIds() == null ? Set.of() : compilation.getEventIds();
        if (ids.isEmpty()) {
            dto.setEvents(Set.of());
            return dto;
        }

        try {
            List<EventShortDto> events = eventInternalClient.getEventsShort(ids);
            dto.setEvents(events.stream().collect(Collectors.toSet()));
        } catch (Exception e) {
            // деградация: подборка без детальной информации о событиях
            dto.setEvents(Set.of());
        }
        return dto;
    }
}

