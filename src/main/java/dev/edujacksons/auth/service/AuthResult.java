package dev.edujacksons.auth.service;

import dev.edujacksons.auth.domain.User;

/** Результат аутентификации: пользователь + выпущенный токен. Контроллер маппит в DTO. */
public record AuthResult(User user, String token, long expiresInSeconds) {
}
