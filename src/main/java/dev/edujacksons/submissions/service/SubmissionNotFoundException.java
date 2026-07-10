package dev.edujacksons.submissions.service;

import dev.edujacksons.common.api.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Решение не найдено. → HTTP 404. */
public class SubmissionNotFoundException extends ApiException {

    public SubmissionNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "submission_not_found", "Решение не найдено: " + id);
    }
}
