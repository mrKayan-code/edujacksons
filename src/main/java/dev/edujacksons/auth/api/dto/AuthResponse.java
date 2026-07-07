package dev.edujacksons.auth.api.dto;

/**
 * Ответ на регистрацию/логин.
 *
 * @param token     access-токен (JWT)
 * @param tokenType схема (всегда {@code Bearer})
 * @param expiresIn срок жизни токена в секундах
 * @param user      данные пользователя
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static AuthResponse bearer(String token, long expiresIn, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresIn, user);
    }
}
