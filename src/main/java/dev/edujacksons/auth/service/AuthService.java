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

    /**
     * Регистрирует нового пользователя в системе.
     * <p>
     * Процесс включает:
     * 1. Нормализацию email (trim и lower-case).
     * 2. Проверку на уникальность email (выбрасывает {@link EmailAlreadyUsedException}, если email занят).
     * 3. Хеширование сырого пароля с помощью {@link PasswordEncoder}.
     * 4. Сохранение пользователя в БД.
     * 5. Мгновенный выпуск JWT-токена для нового пользователя.
     *
     * @param email        email пользователя (будет нормализован)
     * @param rawPassword  сырой пароль (будет захеширован)
     * @param displayName  отображаемое имя пользователя
     * @param role         роль пользователя (TEACHER или STUDENT)
     * @return {@link AuthResult} содержащий созданного пользователя и его токен доступа
     * @throws EmailAlreadyUsedException если пользователь с таким email уже существует
     */
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

    /**
     * Аутентифицирует пользователя по email и паролю.
     * <p>
     * Использует {@link AuthenticationManager} для проверки учетных данных. 
     * Если пароль неверен или пользователь не найден, выбрасывается {@link BadCredentialsException},
     * который перехватывается {@code GlobalExceptionHandler} и возвращает HTTP 401.
     *
     * @param email        email пользователя
     * @param rawPassword  сырой пароль для проверки
     * @return {@link AuthResult} с данными пользователя и новым JWT-токеном
     * @throws BadCredentialsException если учетные данные неверны
     */
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

    /**
     * Возвращает данные пользователя по его уникальному идентификатору.
     * Используется преимущественно для эндпоинта {@code GET /api/auth/me}, 
     * где ID извлекается из JWT-токена.
     *
     * @param id уникальный идентификатор пользователя
     * @return объект {@link User}
     * @throws UserNotFoundException если пользователь с указанным id не найден
     */
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
