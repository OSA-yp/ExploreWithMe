package ru.practicum.explore.server.users.controller;

import lombok.Data;

import java.util.Collection;

@Data
public class GetUsersParams {

    Collection<Long> ids;
    Integer from;
    Integer size;
}
