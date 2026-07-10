package dev.edujacksons.submissions.api.dto;

import dev.edujacksons.judge.api.Verdict;
import dev.edujacksons.submissions.domain.SubmissionResult;
import java.util.UUID;

/** Результат решения по одному тесту (без входа/выхода — скрытые тесты не раскрываем). */
public record SubmissionResultResponse(
        UUID testCaseId,
        Verdict verdict,
        Integer timeMs,
        Integer memoryKb,
        int orderIndex
) {
    public static SubmissionResultResponse from(SubmissionResult r) {
        return new SubmissionResultResponse(r.getTestCaseId(), r.getVerdict(),
                r.getTimeMs(), r.getMemoryKb(), r.getOrderIndex());
    }
}
