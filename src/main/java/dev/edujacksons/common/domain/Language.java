package dev.edujacksons.common.domain;

/**
 * Язык решения задачи. На старте — только {@code PYTHON} (решение из task.md §9: ученики
 * готовятся к ЕГЭ/КЕГЭ, где Python оптимален). Enum и CHECK в БД готовы к расширению.
 * Разделяемый словарь модулей problems/submissions/judge, поэтому живёт в common.
 */
public enum Language {
    PYTHON
}
