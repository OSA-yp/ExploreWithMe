package ru.practicum.explore.server.category.dal;

import ru.practicum.explore.server.category.dto.CategoryResponseDto;
import ru.practicum.explore.server.category.dto.NewCategoryDto;
import ru.practicum.explore.server.category.dto.UpdateCategoryDto;
import ru.practicum.explore.server.category.model.Category;

public class CategoryMapper {

    public static CategoryResponseDto toDto(Category category) {
        return CategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    public static Category toCategory(NewCategoryDto dto) {
        return Category.builder()
                .name(dto.getName())
                .build();
    }

    public static void updateCategory(Category category, UpdateCategoryDto dto) {
        category.setName(dto.getName());
    }

}
