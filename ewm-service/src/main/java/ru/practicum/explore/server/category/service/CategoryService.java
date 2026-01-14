package ru.practicum.explore.server.category.service;

import ru.practicum.explore.server.category.dto.CategoryResponseDto;
import ru.practicum.explore.server.category.dto.NewCategoryDto;
import ru.practicum.explore.server.category.dto.UpdateCategoryDto;

import java.util.Collection;

public interface CategoryService {

    CategoryResponseDto create(NewCategoryDto dto);

    Collection<CategoryResponseDto> getCategories(Collection<Long> ids, Integer from, Integer size);

    CategoryResponseDto getById(Long id);

    CategoryResponseDto update(long id, UpdateCategoryDto dto);

    void delete(long id);
}
