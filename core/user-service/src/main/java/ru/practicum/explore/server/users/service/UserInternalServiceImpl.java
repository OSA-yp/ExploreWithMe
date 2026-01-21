package ru.practicum.explore.server.users.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.explore.server.exception.NotFoundException;
import ru.practicum.explore.server.users.dal.UserMapper;
import ru.practicum.explore.server.users.dal.UserRepository;
import ru.practicum.explore.server.users.dto.UserShortDto;
import ru.practicum.explore.server.users.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserInternalServiceImpl implements UserInternalService {
    private final UserRepository userRepository;

    @Override
    public UserShortDto getUserShort(Long userId) {
        User user = userRepository.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        return UserMapper.toUserShortDto(user);
    }

    @Override
    public List<UserShortDto> getUsersShort(Collection<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> byId = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        return userIds.stream()
                .map(id -> {
                    User u = byId.get(id);
                    if (u == null) {
                        throw new NotFoundException("User with id=" + id + " was not found");
                    }
                    return UserMapper.toUserShortDto(u);
                })
                .toList();
    }
}

