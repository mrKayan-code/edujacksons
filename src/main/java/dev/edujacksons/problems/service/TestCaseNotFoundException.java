package dev.edujacksons.problems.service;

import dev.edujacksons.common.api.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Тест задачи не найден. → HTTP 404. */
public class TestCaseNotFoundException extends ApiException {

    public TestCaseNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "test_case_not_found", "Тест не найден: " + id);
    }
}
