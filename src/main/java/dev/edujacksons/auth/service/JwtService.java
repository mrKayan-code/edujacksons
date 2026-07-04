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

    /** Выпускает access-токен для пользователя. */
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
     * Разбирает и валидирует токен (подпись + срок). Бросает {@link JwtException} при любой проблеме.
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

    /** Разобранные данные токена. */
    public record ParsedToken(UUID userId, String email, Role role) {
    }
}
