package dev.edujacksons.groups.service;

import dev.edujacksons.common.api.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/**
 * Курс нельзя привязать к группе: он не существует или принадлежит другому учителю.
 * Отдаём 404 (не раскрываем факт существования чужого курса).
 */
public class CourseNotAssignableException extends ApiException {

    public CourseNotAssignableException(UUID courseId) {
        super(HttpStatus.NOT_FOUND, "course_not_found", "Курс не найден: " + courseId);
    }
}
