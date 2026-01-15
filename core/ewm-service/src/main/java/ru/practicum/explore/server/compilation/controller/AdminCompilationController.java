package ru.practicum.explore.server.compilation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explore.server.compilation.dto.CompilationDto;
import ru.practicum.explore.server.compilation.dto.NewCompilationDto;
import ru.practicum.explore.server.compilation.dto.UpdateCompilationRequest;
import ru.practicum.explore.server.compilation.service.CompilationService;

@Slf4j
@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
public class AdminCompilationController {

    private final CompilationService compilationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto saveCompilation(@Valid @RequestBody NewCompilationDto newCompilationDto) {
        log.info("Получен запрос на создание подборки: {}.", newCompilationDto);
        return compilationService.saveCompilation(newCompilationDto);
    }

    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompilation(@PathVariable Long compId) {
        log.info("Получен запрос на удаление подборки с id: {}.", compId);
        compilationService.deleteCompilation(compId);
    }

    @PatchMapping("/{compId}")
    @ResponseStatus(HttpStatus.OK)
    public CompilationDto updateCompilation(@PathVariable Long compId,
                                            @Valid @RequestBody UpdateCompilationRequest request) {
        log.info("Получен запрос на изменение категории на: {}.", request);
        return compilationService.updateCompilation(compId, request);
    }
}