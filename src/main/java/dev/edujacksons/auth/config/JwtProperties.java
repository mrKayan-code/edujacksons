package dev.edujacksons.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки JWT.
 *
 * @param secret     HMAC-секрет (≥ 32 байт для HS256). В проде — только из окружения.
 * @param expiration срок жизни access-токена
 * @param issuer     издатель токена (claim {@code iss})
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        Duration expiration,
        String issuer
) {
}
