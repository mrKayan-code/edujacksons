package dev.edujacksons.courses.service;

import java.util.UUID;

/**
 * Публичная граница модуля courses для других модулей.
 * Используется модулем groups при привязке курса к группе (проверка существования и владельца).
 */
public interface CourseDirectory {

    /** Существует ли курс с таким id и принадлежит ли он данному владельцу. */
    boolean isOwnedBy(UUID courseId, UUID ownerId);
}
