package dev.edujacksons.groups.service;

import dev.edujacksons.common.api.ApiException;
import org.springframework.http.HttpStatus;

/** В группу пытаются добавить пользователя, у которого роль не STUDENT. → HTTP 409. */
public class NotAStudentException extends ApiException {

    public NotAStudentException(String email) {
        super(HttpStatus.CONFLICT, "not_a_student", "Пользователь не является учеником: " + email);
    }
}
