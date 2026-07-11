package dev.edujacksons.grading.api.dto;

import dev.edujacksons.submissions.domain.SubmissionStatus;
import dev.edujacksons.judge.api.Verdict;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Строка учительского журнала по задаче.
 * Объединяет данные о решении и его ручной проверке.
 */
public record GradebookEntryResponse(
    UUID submissionId,
    UUID studentId,
    SubmissionStatus submissionStatus,
    Verdict verdict,
    OffsetDateTime submittedAt,
    String reviewStatus, // "NOT_REVIEWED" | "DRAFT" | "PUBLISHED"
    UUID reviewId,
    Integer score
) {}
