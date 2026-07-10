package dev.edujacksons.submissions.service;

import dev.edujacksons.common.api.ApiException;
import org.springframework.http.HttpStatus;

/** Язык решения не совпадает с языком задачи. → HTTP 400. */
public class InvalidLanguageException extends ApiException {

    public InvalidLanguageException(String message) {
        super(HttpStatus.BAD_REQUEST, "invalid_language", message);
    }
}
