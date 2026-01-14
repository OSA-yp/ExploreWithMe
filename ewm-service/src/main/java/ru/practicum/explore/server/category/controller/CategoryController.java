package ru.practicum.explore.server.category.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explore.server.category.dto.CategoryResponseDto;
import ru.practicum.explore.server.category.dto.NewCategoryDto;
import ru.practicum.explore.server.category.dto.UpdateCategoryDto;
import ru.practicum.explore.server.category.service.CategoryService;
import ru.practicum.explore.server.utils.HitSender;

import java.util.Collection;

@RestController
@RequiredArgsConstructor
@Validated
public class CategoryController {
    private final CategoryService categoryService;
    private final HitSender hitSender;

    @PostMapping("/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponseDto create(@RequestBody @Valid NewCategoryDto dto) {
        return categoryService.create(dto);
    }

    @GetMapping("/categories")
    public Collection<CategoryResponseDto> getCategories(@RequestParam(name = "ids", required = false)
                                                         Collection<Long> ids,
                                                         @Valid
                                                         @RequestParam(name = "from", defaultValue = "0", required = false)
                                                         Integer from,
                                                         @RequestParam(name = "size", defaultValue = "10", required = false)
                                                         @Valid
                                                         Integer size) {
        return categoryService.getCategories(ids, from, size);
    }

    @GetMapping("/categories/{catId}")
    public CategoryResponseDto getById(@PathVariable long catId) {
        return categoryService.getById(catId);
    }

    @PatchMapping("/admin/categories/{catId}")
    public CategoryResponseDto update(@PathVariable long catId, @RequestBody @Valid UpdateCategoryDto dto) {
        return categoryService.update(catId, dto);
    }

    @DeleteMapping("/admin/categories/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long catId) {
        categoryService.delete(catId);
    }
}
