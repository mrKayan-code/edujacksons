package dev.edujacksons.auth.service;

import java.util.Optional;
import java.util.UUID;

/**
 * Публичная граница модуля auth для чтения пользователей другими модулями.
 * Внешние модули (например, groups) зависят от этого интерфейса, а не от репозитория/сущности.
 */
public interface UserDirectory {

    /** Ищет пользователя по email (регистр не важен — email нормализуется внутри). */
    Optional<UserView> findByEmail(String email);

    /** Ищет пользователя по id. */
    Optional<UserView> findById(UUID id);
}
