package ru.practicum.explore.server.compilation.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.explore.server.compilation.dto.CompilationDto;
import ru.practicum.explore.server.compilation.dto.NewCompilationDto;
import ru.practicum.explore.server.compilation.model.Compilation;
import ru.practicum.explore.server.event.dto.EventShortDto;
import ru.practicum.explore.server.event.mapper.EventMapper;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CompilationMapper {

    public static CompilationDto toDto(Compilation compilation) {
        if (compilation == null) {
            return null;
        }

        CompilationDto dto = new CompilationDto();

        dto.setId(compilation.getId() != null ? compilation.getId().longValue() : null);
        dto.setTitle(compilation.getTitle());
        dto.setPinned(compilation.isPinned());

        if (compilation.getEvents() != null) {
            Set<EventShortDto> eventDtos = compilation.getEvents().stream()
                    .map(EventMapper::toEventShortDto)
                    .collect(Collectors.toSet());
            dto.setEvents(eventDtos);
        }

        return dto;
    }

    public static Compilation toEntity(NewCompilationDto dto) {
        if (dto == null) {
            return null;
        }

        Compilation compilation = new Compilation();
        compilation.setTitle(dto.getTitle());
        compilation.setPinned(dto.getPinned() != null && dto.getPinned());

        return compilation;
    }
}