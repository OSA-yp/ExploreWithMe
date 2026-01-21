package ru.practicum.explore.server.users.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.explore.server.users.dto.UserShortDto;

import java.util.Collection;
import java.util.List;

@FeignClient(name = "user-service", path = "/internal/users")
public interface UserInternalClient {
    @GetMapping("/{userId}")
    UserShortDto getUser(@PathVariable("userId") Long userId);

    @GetMapping
    List<UserShortDto> getUsers(@RequestParam("ids") Collection<Long> ids);
}

