package dev.edujacksons.judge.service;

/** Как отработал процесс исполнения кода (без сверки вывода — это уже дело {@link JudgeService}). */
public enum ExecutionStatus {
    OK,
    TIME_LIMIT,
    RUNTIME_ERROR,
    COMPILE_ERROR,
    INTERNAL_ERROR
}
