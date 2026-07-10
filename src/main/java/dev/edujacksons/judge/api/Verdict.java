package dev.edujacksons.judge.api;

/**
 * Вердикт проверки — на уровне отдельного теста и итоговый по решению. Часть публичного
 * контракта модуля judge (передаётся в {@link JudgeResult} через Kafka). Модуль submissions
 * переиспользует этот enum как значение своего поля — отдельного дубля не заводим.
 */
public enum Verdict {
    ACCEPTED,
    WRONG_ANSWER,
    TIME_LIMIT_EXCEEDED,
    RUNTIME_ERROR,
    COMPILE_ERROR,
    INTERNAL_ERROR
}
