package ru.practicum.explore.server.users.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.explore.server.users.dto.UserShortDto;
import ru.practicum.explore.server.users.service.UserInternalService;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUsersController {
    private final UserInternalService userInternalService;

    @GetMapping("/{userId}")
    public UserShortDto getUser(@PathVariable Long userId) {
        return userInternalService.getUserShort(userId);
    }

    @GetMapping
    public List<UserShortDto> getUsers(@RequestParam(name = "ids") Collection<Long> ids) {
        return userInternalService.getUsersShort(ids);
    }
}

