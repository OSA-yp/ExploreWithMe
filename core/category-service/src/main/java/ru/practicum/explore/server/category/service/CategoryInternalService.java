package ru.practicum.explore.server.category.service;

import ru.practicum.explore.server.category.dto.CategoryResponseDto;

import java.util.Collection;
import java.util.List;

public interface CategoryInternalService {
    CategoryResponseDto getCategory(Long id);

    List<CategoryResponseDto> getCategories(Collection<Long> ids);
}

