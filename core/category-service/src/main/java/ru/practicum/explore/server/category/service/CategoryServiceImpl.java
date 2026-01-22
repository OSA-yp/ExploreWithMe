package ru.practicum.explore.server.category.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.practicum.explore.server.category.dal.CategoryMapper;
import ru.practicum.explore.server.category.dal.CategoryRepository;
import ru.practicum.explore.server.category.dto.CategoryResponseDto;
import ru.practicum.explore.server.category.dto.NewCategoryDto;
import ru.practicum.explore.server.category.dto.UpdateCategoryDto;
import ru.practicum.explore.server.category.model.Category;
import ru.practicum.explore.server.exception.AppException;
import ru.practicum.explore.server.exception.ConflictException;
import ru.practicum.explore.server.exception.NotFoundException;
import ru.practicum.explore.server.exception.ValidationException;
import ru.practicum.explore.server.event.client.EventInternalClient;

import java.util.Collection;

@Service
@AllArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventInternalClient eventInternalClient;

    @Override
    public CategoryResponseDto create(NewCategoryDto dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new ConflictException("Категория с таким именем уже есть");
        }
        Category newCategory = categoryRepository.save(CategoryMapper.toCategory(dto));
        log.info("Category with id={} was created", newCategory.getId());
        return CategoryMapper.toDto(newCategory);
    }

    @Override
    public Collection<CategoryResponseDto> getCategories(Collection<Long> ids, Integer from, Integer size) {
        int safeFrom = from == null ? 0 : from;
        int safeSize = size == null ? 10 : size;
        if (safeFrom < 0 || safeSize <= 0) {
            throw new AppException("Ошибка: некорректные параметры пагинации", HttpStatus.BAD_REQUEST);
        }

        if (ids != null && !ids.isEmpty()) {
            Collection<Category> categories = categoryRepository.findAllById(ids);
            return categories.stream()
                    .map(CategoryMapper::toDto)
                    .toList();
        }

        Pageable pageable = PageRequest.of(safeFrom / safeSize, safeSize);
        Page<Category> categories = categoryRepository.findAll(pageable);
        return categories.stream()
                .map(CategoryMapper::toDto)
                .toList();
    }

    @Override
    public CategoryResponseDto getById(Long id) {
        Category category = findCategoryById(id);
        return CategoryMapper.toDto(category);
    }

    @Override
    public CategoryResponseDto update(long id, UpdateCategoryDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id = " + id + " не найдена"));
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(dto.getName(), id)) {
            throw new ConflictException("Категория с таким именем уже есть");
        }
        CategoryMapper.updateCategory(category, dto);
        log.info("Category with id={} was updated", category.getId());
        return CategoryMapper.toDto(categoryRepository.save(category));
    }

    @Override
    public void delete(long id) {
        findCategoryById(id);

        boolean hasEvents;
        try {
            hasEvents = eventInternalClient.existsByCategory(id);
        } catch (Exception e) {
            // Надёжность: при недоступности сервиса событий ведём себя консервативно и не удаляем категорию
            hasEvents = true;
        }

        if (hasEvents) {
            throw new ConflictException("Нельзя удалить категорию, к которой привязаны события");
        }

        categoryRepository.deleteById(id);
        log.info("Category with id={} was deleted", id);
    }

    private Category findCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id = " + id + " не найдена"));
    }
}

