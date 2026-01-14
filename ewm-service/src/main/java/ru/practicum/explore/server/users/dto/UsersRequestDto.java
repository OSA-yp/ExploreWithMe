package ru.practicum.explore.server.users.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.Collection;

@Data
public class UsersRequestDto {

    private Collection<Long> ids;

    @Positive
    private Integer from;

    @Positive
    private Integer size;
}
