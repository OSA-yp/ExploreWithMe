package ru.practicum.explore.server.category.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.explore.server.category.dal.CategoryMapper;
import ru.practicum.explore.server.category.dal.CategoryRepository;
import ru.practicum.explore.server.category.dto.CategoryResponseDto;
import ru.practicum.explore.server.category.dto.NewCategoryDto;
import ru.practicum.explore.server.category.dto.UpdateCategoryDto;
import ru.practicum.explore.server.category.model.Category;
import ru.practicum.explore.server.event.repository.EventRepository;
import ru.practicum.explore.server.exception.ConflictException;
import ru.practicum.explore.server.exception.NotFoundException;

import java.util.Collection;

@Service
@AllArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

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
        Pageable pageable = PageRequest.of(from, size);

        if (ids != null && !ids.isEmpty()) {
            Collection<Category> categories = categoryRepository.findAllById(ids);
            return categories.stream()
                    .map(CategoryMapper::toDto)
                    .toList();
        } else {
            Page<Category> categories = categoryRepository.findAll(pageable);
            return categories.stream()
                    .map(CategoryMapper::toDto)
                    .toList();
        }
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
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id = " + id + " не найдена"));

        if (eventRepository.existsByCategory_Id(id)) {
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
