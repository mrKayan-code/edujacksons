package dev.edujacksons.grading.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Усечённый ответ по проверке (для STUDENT).
 * Содержит только данные опубликованной проверки.
 */
/**
 * Усечённый ответ по проверке (предназначен для STUDENT).
 */
public record StudentReviewResponse(
    UUID submissionId,
    Integer score,
    String feedback,
    OffsetDateTime publishedAt
) {}
