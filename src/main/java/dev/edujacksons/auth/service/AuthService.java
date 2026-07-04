package dev.edujacksons.auth.service;

import dev.edujacksons.auth.domain.Role;
import dev.edujacksons.auth.domain.User;
import dev.edujacksons.auth.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Публичный сервис модуля auth: регистрация, логин, чтение текущего пользователя.
 * Это единственная точка входа в модуль для остального кода.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /** Регистрирует нового пользователя и сразу выпускает токен. */
    @Transactional
    public AuthResult register(String email, String rawPassword, String displayName, Role role) {
        String normalized = normalizeEmail(email);
        if (userRepository.existsByEmail(normalized)) {
            throw new EmailAlreadyUsedException(normalized);
        }
        User user = new User(normalized, passwordEncoder.encode(rawPassword), role, displayName);
        User saved = userRepository.save(user);
        return issue(saved);
    }

    /** Проверяет учётные данные через {@link AuthenticationManager} и выпускает токен. */
    @Transactional(readOnly = true)
    public AuthResult login(String email, String rawPassword) {
        String normalized = normalizeEmail(email);
        // Бросает BadCredentialsException при неверных данных → 401 в GlobalExceptionHandler.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalized, rawPassword));
        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new BadCredentialsException("Неверный email или пароль"));
        return issue(user);
    }

    /** Текущий пользователь по id из токена (для {@code GET /api/auth/me}). */
    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private AuthResult issue(User user) {
        return new AuthResult(user, jwtService.issueToken(user), jwtService.expiresInSeconds());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
