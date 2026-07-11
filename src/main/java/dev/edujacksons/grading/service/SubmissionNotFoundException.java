package dev.edujacksons.grading.service;

import dev.edujacksons.common.api.ApiException;
import org.springframework.http.HttpStatus;

/** Решение не найдено. → HTTP 404. */
public class SubmissionNotFoundException extends ApiException {
    public SubmissionNotFoundException() {
        super(HttpStatus.NOT_FOUND, "submission_not_found", "Решение не найдено");
    }
}
