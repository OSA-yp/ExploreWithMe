package ru.practicum.explore.server.compilation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.explore.server.compilation.model.Compilation;

@Repository
public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    Page<Compilation> findByPinned(boolean pinned, Pageable pageable);

    boolean existsByTitle(String title);

}