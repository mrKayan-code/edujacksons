package dev.edujacksons.common.api;

import org.springframework.http.HttpStatus;

/** Доступ к чужому ресурсу (не владелец / нет доступа к курсу). → HTTP 403. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "forbidden", message);
    }
}
