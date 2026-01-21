package ru.practicum.explore.server.users.controller;

import lombok.Data;

import java.util.Collection;

@Data
public class GetUsersParams {
    private Collection<Long> ids;
    private Integer from;
    private Integer size;
}

