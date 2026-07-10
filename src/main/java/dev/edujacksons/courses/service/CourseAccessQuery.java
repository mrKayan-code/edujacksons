package dev.edujacksons.courses.service;

import java.util.Set;
import java.util.UUID;

/**
 * Порт, который объявляет модуль courses: какие курсы доступны ученику через его группы.
 * Реализуется модулем groups ({@code CourseAccessQueryImpl}). Интерфейс живёт у потребителя
 * (DIP), поэтому courses не зависит от groups — граф пакетов ацикличен (groups → courses).
 * Используется при гейте чтения материалов учеником.
 */
public interface CourseAccessQuery {

    /** Id курсов, доступных ученику через членство в группах. */
    Set<UUID> accessibleCourseIds(UUID studentId);

    /** Есть ли у ученика доступ к конкретному курсу. */
    boolean canAccess(UUID studentId, UUID courseId);
}
