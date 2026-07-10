package dev.edujacksons.problems.api.dto;

import dev.edujacksons.common.domain.Language;
import dev.edujacksons.problems.domain.Problem;
import dev.edujacksons.problems.domain.TestCase;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Полная карточка задачи: условие + видимые тесты. Список {@code tests} формирует контроллер —
 * владельцу отдаёт все тесты, ученику только открытые (sample); скрытые тесты наружу не уходят.
 */
public record ProblemDetailResponse(
        UUID id,
        UUID courseId,
        UUID ownerId,
        String title,
        String statement,
        Language language,
        int timeLimitMs,
        int memoryLimitKb,
        Instant createdAt,
        List<TestCaseResponse> tests
) {
    public static ProblemDetailResponse from(Problem p, List<TestCase> visibleTests) {
        return new ProblemDetailResponse(p.getId(), p.getCourseId(), p.getOwnerId(), p.getTitle(),
                p.getStatement(), p.getLanguage(), p.getTimeLimitMs(), p.getMemoryLimitKb(),
                p.getCreatedAt(), visibleTests.stream().map(TestCaseResponse::from).toList());
    }
}
