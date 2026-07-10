package dev.edujacksons.auth.service;

import dev.edujacksons.auth.config.JwtProperties;
import dev.edujacksons.auth.domain.Role;
import dev.edujacksons.auth.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Выпуск и валидация JWT (HS256). Subject токена — id пользователя. */
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Создает подписанный JWT-токен доступа для пользователя.
     * <p>
     * В payload токена записываются следующие claims:
     * - {@code sub} (Subject): ID пользователя (UUID)
     * - {@code email}: email пользователя
     * - {@code role}: роль пользователя (строковое представление)
     * - {@code iat}: время выпуска
     * - {@code exp}: время истечения (из настроек {@link JwtProperties})
     *
     * @param user пользователь, для которого выпускается токен
     * @return строка JWT-токена в формате Header.Payload.Signature
     */
    public String issueToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.expiration());
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /** Срок жизни токена в секундах (для ответа клиенту). */
    public long expiresInSeconds() {
        return properties.expiration().toSeconds();
    }

    /**
     * Декодирует JWT-токен, проверяет его цифровую подпись и срок действия.
     * <p>
     * Если подпись не совпадает с секретным ключом или токен просрочен,
     * библиотека io.jsonwebtoken выбрасывает {@link JwtException}.
     *
     * @param token строка JWT-токена
     * @return {@link ParsedToken} с данными, извлеченными из claims
     * @throws JwtException если токен невалиден, поврежден или просрочен
     */
    public ParsedToken parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new ParsedToken(
                UUID.fromString(claims.getSubject()),
                claims.get(CLAIM_EMAIL, String.class),
                Role.valueOf(claims.get(CLAIM_ROLE, String.class))
        );
    }

    /**
     * Контейнер для данных, извлеченных из валидного JWT-токена.
     *
     * @param userId уникальный идентификатор пользователя (из {@code sub} claim)
     * @param email  email пользователя (из кастомного {@code email} claim)
     * @param role   роль пользователя (из кастомного {@code role} claim)
     */
    public record ParsedToken(UUID userId, String email, Role role) {
    }
}
