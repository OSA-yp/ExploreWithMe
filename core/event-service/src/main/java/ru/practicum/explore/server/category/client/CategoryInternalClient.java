package ru.practicum.explore.server.category.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.explore.server.category.dto.CategoryResponseDto;

import java.util.Collection;
import java.util.List;

@FeignClient(name = "category-service", path = "/internal/categories")
public interface CategoryInternalClient {
    @GetMapping("/{catId}")
    CategoryResponseDto getCategory(@PathVariable("catId") Long catId);

    @GetMapping
    List<CategoryResponseDto> getCategories(@RequestParam("ids") Collection<Long> ids);
}

