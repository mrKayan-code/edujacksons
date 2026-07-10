package dev.edujacksons.judge.service;

/**
 * Граница исполнения кода. Только <b>запускает</b> код на одном входе и возвращает stdout +
 * статус/метрики — сверку вывода с ожидаемым делает {@link JudgeService} (так логика вердикта
 * тестируема и не зависит от бэкенда исполнения). Реализация на старте — Judge0 за HTTP
 * ({@code Judge0CodeExecutor}); позже возможен свой sandbox — интерфейс не изменится.
 */
public interface CodeExecutor {

    ExecutionResult execute(ExecutionRequest request);
}
