package dev.edujacksons.problems.api.dto;

import dev.edujacksons.common.domain.Language;
import dev.edujacksons.problems.domain.Problem;
import java.time.Instant;
import java.util.UUID;

/** Краткая карточка задачи (без тестов и условия) — для списков. */
public record ProblemResponse(
        UUID id,
        UUID courseId,
        UUID ownerId,
        String title,
        Language language,
        int timeLimitMs,
        int memoryLimitKb,
        Instant createdAt
) {
    public static ProblemResponse from(Problem p) {
        return new ProblemResponse(p.getId(), p.getCourseId(), p.getOwnerId(), p.getTitle(),
                p.getLanguage(), p.getTimeLimitMs(), p.getMemoryLimitKb(), p.getCreatedAt());
    }
}
