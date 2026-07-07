package dev.edujacksons.auth.service;

import dev.edujacksons.auth.domain.User;
import dev.edujacksons.auth.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Реализация публичной границы {@link UserDirectory} поверх {@link UserRepository}. */
@Service
class UserDirectoryImpl implements UserDirectory {

    private final UserRepository userRepository;

    UserDirectoryImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserView> findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase()).map(UserDirectoryImpl::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserView> findById(UUID id) {
        return userRepository.findById(id).map(UserDirectoryImpl::toView);
    }

    private static UserView toView(User user) {
        return new UserView(user.getId(), user.getEmail(), user.getRole(), user.getDisplayName());
    }
}
