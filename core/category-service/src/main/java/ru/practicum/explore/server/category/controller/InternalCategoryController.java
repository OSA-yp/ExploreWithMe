package ru.practicum.explore.server.category.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.explore.server.category.dto.CategoryResponseDto;
import ru.practicum.explore.server.category.service.CategoryInternalService;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/internal/categories")
@RequiredArgsConstructor
public class InternalCategoryController {
    private final CategoryInternalService categoryInternalService;

    @GetMapping("/{catId}")
    public CategoryResponseDto getCategory(@PathVariable Long catId) {
        return categoryInternalService.getCategory(catId);
    }

    @GetMapping
    public List<CategoryResponseDto> getCategories(@RequestParam(name = "ids") Collection<Long> ids) {
        return categoryInternalService.getCategories(ids);
    }
}

