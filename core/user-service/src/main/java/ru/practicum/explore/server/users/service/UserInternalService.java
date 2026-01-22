package ru.practicum.explore.server.users.service;

import ru.practicum.explore.server.users.dto.UserShortDto;

import java.util.Collection;
import java.util.List;

public interface UserInternalService {
    UserShortDto getUserShort(Long userId);

    List<UserShortDto> getUsersShort(Collection<Long> userIds);
}

