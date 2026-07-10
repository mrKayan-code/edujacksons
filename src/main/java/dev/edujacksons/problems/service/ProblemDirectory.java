package dev.edujacksons.problems.service;

import java.util.Optional;
import java.util.UUID;

/**
 * Публичная граница модуля problems для модуля submissions. Зависимость односторонняя
 * (submissions → problems), поэтому порт живёт у провайдера (ADR 0004). Реализует тонкий
 * бин-адаптер {@code ProblemDirectoryImpl} над репозиториями + {@code CourseAccessQuery},
 * не {@code ProblemService} — чтобы граф бинов оставался ацикличным.
 */
public interface ProblemDirectory {

    /** Может ли пользователь отправлять решение задачи: владелец-учитель или ученик с доступом к её курсу. */
    boolean canAccess(UUID userId, UUID problemId);

    /** Является ли пользователь владельцем задачи (учитель). Нужен для доступа учителя к чужим решениям. */
    boolean ownsProblem(UUID userId, UUID problemId);

    /** Спецификация проверки задачи (язык, лимиты, все тесты) — для сборки заявки на проверку. */
    Optional<ProblemExecutionSpec> executionSpec(UUID problemId);
}
