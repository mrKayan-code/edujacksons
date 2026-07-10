package dev.edujacksons.courses.domain;

/**
 * Тип материала курса. В Фазе 1 реализован только {@code LECTURE} (markdown-лекция со слайдами,
 * разделёнными {@code ---}). Файловые/ссылочные типы (FILE/VIDEO/LINK) — позже, когда решим
 * про файловое хранилище; enum и CHECK в БД к расширению готовы.
 */
public enum MaterialType {
    LECTURE
}
