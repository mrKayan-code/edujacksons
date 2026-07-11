package dev.edujacksons.grading.api.dto;

/**
 * Запрос на обновление существующей проверки решения.
 * Используется patch-семантика: присланные поля перезаписывают текущие.
 */
public record UpdateReviewRequest(
    Integer score,
    String feedback,
    Boolean publish
) {}
