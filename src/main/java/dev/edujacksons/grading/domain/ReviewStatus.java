package dev.edujacksons.grading.domain;

/**
 * Статус ручной проверки.
 * DRAFT — черновик учителя, невидим ученику.
 * PUBLISHED — опубликованная оценка и фидбэк, видны ученику.
 */
public enum ReviewStatus {
    DRAFT,
    PUBLISHED
}
