package ru.practicum.explore.server.users.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.explore.server.users.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> getUserById(Long id);

    Optional<User> getUserByEmail(String email);
}
