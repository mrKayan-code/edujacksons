package dev.edujacksons.auth.service;

import dev.edujacksons.auth.domain.Role;
import java.util.UUID;

/**
 * Публичное представление пользователя для других модулей.
 * Отдаётся вместо JPA-сущности {@code User}, чтобы не протекала внутренняя модель auth.
 */
public record UserView(UUID id, String email, Role role, String displayName) {
}
