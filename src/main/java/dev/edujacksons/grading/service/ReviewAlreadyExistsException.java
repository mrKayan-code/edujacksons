package dev.edujacksons.grading.service;

import dev.edujacksons.common.api.ApiException;
import org.springframework.http.HttpStatus;

/** Для данного решения уже создана проверка. → HTTP 409. */
public class ReviewAlreadyExistsException extends ApiException {
    public ReviewAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "review_exists", "Для этого решения уже существует проверка");
    }
}
