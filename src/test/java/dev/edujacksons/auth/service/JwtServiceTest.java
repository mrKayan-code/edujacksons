package dev.edujacksons.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.edujacksons.auth.config.JwtProperties;
import dev.edujacksons.auth.domain.Role;
import dev.edujacksons.auth.domain.User;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Юнит-тесты выпуска/валидации JWT — без контекста Spring и без Docker. */
class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256-32b";

    private JwtService service(Duration expiration) {
        return new JwtService(new JwtProperties(SECRET, expiration, "edujacksons-test"));
    }

    private User user(UUID id, String email, Role role) {
        User user = new User(email, "hash", role, "Имя");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void issuedTokenParsesBackToSameClaims() {
        JwtService jwt = service(Duration.ofHours(1));
        UUID id = UUID.randomUUID();
        User user = user(id, "teacher@example.com", Role.TEACHER);

        String token = jwt.issueToken(user);
        JwtService.ParsedToken parsed = jwt.parse(token);

        assertThat(parsed.userId()).isEqualTo(id);
        assertThat(parsed.email()).isEqualTo("teacher@example.com");
        assertThat(parsed.role()).isEqualTo(Role.TEACHER);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = service(Duration.ofSeconds(-10));
        String token = jwt.issueToken(user(UUID.randomUUID(), "s@example.com", Role.STUDENT));

        assertThatThrownBy(() -> jwt.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = service(Duration.ofHours(1));
        String token = jwt.issueToken(user(UUID.randomUUID(), "s@example.com", Role.STUDENT));

        assertThatThrownBy(() -> jwt.parse(token + "x")).isInstanceOf(JwtException.class);
    }
}
