package ru.practicum.explore.server.users.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explore.server.users.dal.UserMapper;
import ru.practicum.explore.server.users.dto.NewUserRequestDto;
import ru.practicum.explore.server.users.dto.UserResponseDto;
import ru.practicum.explore.server.users.service.UserService;

import java.util.Collection;

@RestController
@AllArgsConstructor
public class UsersController {

    private final UserService userService;


    @PostMapping("/admin/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto addUser(
            @Valid
            @RequestBody
            NewUserRequestDto newUser) {

        return userService.addUser(UserMapper.toUser(newUser));
    }

    @GetMapping("/admin/users")
    public Collection<UserResponseDto> getUsers(

            @RequestParam(name = "ids", required = false)
            Collection<Long> ids,

            @Valid
            @RequestParam(name = "from", defaultValue = "0", required = false)
            Integer from,

            @RequestParam(name = "size", defaultValue = "10", required = false)
            @Valid
            Integer size) {


        GetUsersParams getUsersParams = new GetUsersParams();
        getUsersParams.setIds(ids);
        getUsersParams.setFrom(from);
        getUsersParams.setSize(size);

        return userService.getUsers(getUsersParams);

    }

    @DeleteMapping("/admin/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserById(@PathVariable Long userId) {

        userService.deleteUserById(userId);

    }

}
