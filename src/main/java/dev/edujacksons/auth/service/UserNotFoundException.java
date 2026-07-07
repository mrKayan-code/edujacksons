package dev.edujacksons.auth.service;

import dev.edujacksons.common.api.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Пользователь по id не найден (например, токен ссылается на удалённого пользователя). */
public class UserNotFoundException extends ApiException {

    public UserNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "user_not_found", "Пользователь не найден: " + id);
    }
}
