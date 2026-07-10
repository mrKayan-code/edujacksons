package dev.edujacksons.submissions.api.dto;

import dev.edujacksons.common.domain.Language;
import dev.edujacksons.judge.api.Verdict;
import dev.edujacksons.submissions.domain.Submission;
import dev.edujacksons.submissions.domain.SubmissionResult;
import dev.edujacksons.submissions.domain.SubmissionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Полная карточка решения: статус/вердикт + разбивка по тестам. */
public record SubmissionDetailResponse(
        UUID id,
        UUID problemId,
        UUID studentId,
        Language language,
        SubmissionStatus status,
        Verdict verdict,
        Integer score,
        Integer totalTests,
        Instant createdAt,
        List<SubmissionResultResponse> results
) {
    public static SubmissionDetailResponse from(Submission s, List<SubmissionResult> results) {
        return new SubmissionDetailResponse(s.getId(), s.getProblemId(), s.getStudentId(),
                s.getLanguage(), s.getStatus(), s.getVerdict(), s.getScore(), s.getTotalTests(),
                s.getCreatedAt(), results.stream().map(SubmissionResultResponse::from).toList());
    }
}
