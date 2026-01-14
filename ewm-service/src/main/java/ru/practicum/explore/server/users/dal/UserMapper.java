package ru.practicum.explore.server.users.dal;

import ru.practicum.explore.server.users.dto.NewUserRequestDto;
import ru.practicum.explore.server.users.dto.UserResponseDto;
import ru.practicum.explore.server.users.dto.UserShortDto;
import ru.practicum.explore.server.users.model.User;

public class UserMapper {

    public static UserResponseDto toUserResponseDto(User user) {

        UserResponseDto dto = new UserResponseDto();

        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());

        return dto;
    }

    public static User toUser(NewUserRequestDto dto) {

        User user = new User();

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());

        return user;
    }

    public static UserShortDto toUserShortDto(User user) {

        UserShortDto dto = new UserShortDto();

        dto.setId(user.getId());
        dto.setName(user.getName());

        return dto;
    }
}
