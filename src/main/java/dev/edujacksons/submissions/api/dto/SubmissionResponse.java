package dev.edujacksons.submissions.api.dto;

import dev.edujacksons.common.domain.Language;
import dev.edujacksons.judge.api.Verdict;
import dev.edujacksons.submissions.domain.Submission;
import dev.edujacksons.submissions.domain.SubmissionStatus;
import java.time.Instant;
import java.util.UUID;

/** Карточка решения: статус жизненного цикла + вердикт/счёт (заполнены после проверки). */
public record SubmissionResponse(
        UUID id,
        UUID problemId,
        UUID studentId,
        Language language,
        SubmissionStatus status,
        Verdict verdict,
        Integer score,
        Integer totalTests,
        Instant createdAt
) {
    public static SubmissionResponse from(Submission s) {
        return new SubmissionResponse(s.getId(), s.getProblemId(), s.getStudentId(), s.getLanguage(),
                s.getStatus(), s.getVerdict(), s.getScore(), s.getTotalTests(), s.getCreatedAt());
    }
}
