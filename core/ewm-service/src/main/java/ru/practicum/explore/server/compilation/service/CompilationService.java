package ru.practicum.explore.server.compilation.service;

import ru.practicum.explore.server.compilation.dto.CompilationDto;
import ru.practicum.explore.server.compilation.dto.NewCompilationDto;
import ru.practicum.explore.server.compilation.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    CompilationDto saveCompilation(NewCompilationDto newCompilationDto);

    List<CompilationDto> getCompilations(Boolean pinned, int from, int size);

    CompilationDto getCompilationById(Long compId);

    void deleteCompilation(Long compId);

    CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request);
}