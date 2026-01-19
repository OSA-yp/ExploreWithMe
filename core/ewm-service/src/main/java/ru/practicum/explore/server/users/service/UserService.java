package ru.practicum.explore.server.users.service;

import ru.practicum.explore.server.users.controller.GetUsersParams;
import ru.practicum.explore.server.users.dto.UserResponseDto;
import ru.practicum.explore.server.users.model.User;

import java.util.Collection;

public interface UserService {
    UserResponseDto addUser(User user);

    Collection<UserResponseDto> getUsers(GetUsersParams params);

    void deleteUserById(Long userId);

    void checkUser(Long userId);
}
