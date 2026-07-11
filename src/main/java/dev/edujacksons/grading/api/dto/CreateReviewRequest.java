package dev.edujacksons.grading.api.dto;

import java.util.UUID;

/**
 * Запрос на создание проверки решения.
 */
/**
 * Запрос на создание проверки решения.
 */
public record CreateReviewRequest(
    Integer score,
    String feedback,
    boolean publish
) {}
