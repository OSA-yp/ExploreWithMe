package ru.practicum.explore.server.category.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.explore.server.category.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Category getCategoryById(Long catId);
}
