package dev.edujacksons.groups.service;

import java.util.Set;
import java.util.UUID;

/**
 * Публичная граница модуля groups: какие курсы доступны ученику через его группы.
 * Используется модулем courses для гейта чтения материалов учеником.
 */
public interface CourseAccessQuery {

    /** Id курсов, доступных ученику через членство в группах. */
    Set<UUID> accessibleCourseIds(UUID studentId);

    /** Есть ли у ученика доступ к конкретному курсу. */
    boolean canAccess(UUID studentId, UUID courseId);
}
