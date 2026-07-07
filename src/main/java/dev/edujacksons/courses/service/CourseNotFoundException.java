package dev.edujacksons.courses.service;

import dev.edujacksons.common.api.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Курс не найден. → HTTP 404. */
public class CourseNotFoundException extends ApiException {

    public CourseNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "course_not_found", "Курс не найден: " + id);
    }
}
