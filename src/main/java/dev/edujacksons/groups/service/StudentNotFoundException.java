package dev.edujacksons.groups.service;

import dev.edujacksons.common.api.ApiException;
import org.springframework.http.HttpStatus;

/** В группу добавляют email, которого нет среди зарегистрированных. → HTTP 404. */
public class StudentNotFoundException extends ApiException {

    public StudentNotFoundException(String email) {
        super(HttpStatus.NOT_FOUND, "student_not_found", "Ученик с таким email не найден: " + email);
    }
}
