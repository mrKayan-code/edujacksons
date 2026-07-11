package dev.edujacksons.grading.api.dto;

/**
 * Запрос на обновление существующей проверки решения.
 * Используется patch-семантика: присланные поля перезаписывают текущие.
 */
/**
 * Запрос на частичное обновление существующей проверки решения.
 */
public record UpdateReviewRequest(
    Integer score,
    String feedback,
    Boolean publish
) {}
