package dev.edujacksons.grading.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Усечённый ответ по проверке (для STUDENT).
 * Содержит только данные опубликованной проверки.
 */
public record StudentReviewResponse(
    UUID submissionId,
    Integer score,
    String feedback,
    OffsetDateTime publishedAt
) {}
