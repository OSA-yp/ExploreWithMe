package ru.practicum.explore.server.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.explore.server.category.dal.CategoryMapper;
import ru.practicum.explore.server.category.dal.CategoryRepository;
import ru.practicum.explore.server.category.dto.CategoryResponseDto;
import ru.practicum.explore.server.category.model.Category;
import ru.practicum.explore.server.exception.NotFoundException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryInternalServiceImpl implements CategoryInternalService {
    private final CategoryRepository categoryRepository;

    @Override
    public CategoryResponseDto getCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id = " + id + " не найдена"));
        return CategoryMapper.toDto(category);
    }

    @Override
    public List<CategoryResponseDto> getCategories(Collection<Long> ids) {
        List<Category> categories = categoryRepository.findAllById(ids);
        Map<Long, Category> byId = categories.stream().collect(Collectors.toMap(Category::getId, c -> c));
        return ids.stream()
                .map(id -> {
                    Category c = byId.get(id);
                    if (c == null) {
                        throw new NotFoundException("Категория с id = " + id + " не найдена");
                    }
                    return CategoryMapper.toDto(c);
                })
                .toList();
    }
}

