package dev.edujacksons.problems.service;

import dev.edujacksons.common.api.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Задача не найдена. → HTTP 404. */
public class ProblemNotFoundException extends ApiException {

    public ProblemNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "problem_not_found", "Задача не найдена: " + id);
    }
}
