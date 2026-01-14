package ru.practicum.explore.server.users.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.explore.server.exception.ConflictException;
import ru.practicum.explore.server.exception.NotFoundException;
import ru.practicum.explore.server.users.controller.GetUsersParams;
import ru.practicum.explore.server.users.dal.UserMapper;
import ru.practicum.explore.server.users.dal.UserRepository;
import ru.practicum.explore.server.users.dto.UserResponseDto;
import ru.practicum.explore.server.users.model.User;

import java.util.Collection;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponseDto addUser(User user) {

        checkEmailExisting(user.getEmail());

        User newUser = userRepository.save(user);

        log.info("User with id={} was created", newUser.getId());

        return UserMapper.toUserResponseDto(newUser);
    }


    @Override
    public Collection<UserResponseDto> getUsers(GetUsersParams params) {

        Pageable pageable = PageRequest.of(params.getFrom(), params.getSize());

        if (params.getIds() != null && !params.getIds().isEmpty()) {
            Collection<User> users = userRepository.findAllById(params.getIds());
            return users.stream()
                    .map(UserMapper::toUserResponseDto)
                    .toList();
        } else {

            Page<User> users = userRepository.findAll(pageable);
            return users.stream()
                    .map(UserMapper::toUserResponseDto)
                    .toList();
        }
    }

    @Override
    public void deleteUserById(Long userId) {

        checkUser(userId);

        userRepository.deleteById(userId);

        log.info("User with id={} was deleted", userId);
    }

    public void checkUser(Long userId) {

        Optional<User> maybeUser = userRepository.getUserById(userId);

        if (maybeUser.isEmpty()) {
            throw new NotFoundException("User with id=" + userId + " was not found");

        }
    }

    private void checkEmailExisting(String email) {

        Optional<User> maybeUser = userRepository.getUserByEmail(email);

        if (maybeUser.isPresent()) {
            throw new ConflictException("User with email " + email + " already exist");
        }
    }
}
