package dev.edujacksons.grading.api.dto;

import dev.edujacksons.grading.domain.ReviewStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Полный ответ по проверке (для TEACHER).
 */
/**
 * Полный ответ по проверке (предназначен для TEACHER).
 */
public record ReviewResponse(
    UUID id,
    UUID submissionId,
    UUID problemId,
    UUID studentId,
    UUID reviewerId,
    ReviewStatus status,
    Integer score,
    String feedback,
    OffsetDateTime publishedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
