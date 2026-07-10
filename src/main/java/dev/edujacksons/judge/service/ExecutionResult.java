package dev.edujacksons.judge.service;

/**
 * Итог одного прогона. {@code status} — как отработал процесс (без сверки вывода);
 * {@code stdout} значим только при {@link ExecutionStatus#OK}. Метрики могут быть {@code null}.
 */
public record ExecutionResult(
        ExecutionStatus status,
        String stdout,
        Integer timeMs,
        Integer memoryKb,
        String message
) {
    public static ExecutionResult ok(String stdout, Integer timeMs, Integer memoryKb) {
        return new ExecutionResult(ExecutionStatus.OK, stdout, timeMs, memoryKb, null);
    }

    public static ExecutionResult failed(ExecutionStatus status, String message) {
        return new ExecutionResult(status, null, null, null, message);
    }
}
