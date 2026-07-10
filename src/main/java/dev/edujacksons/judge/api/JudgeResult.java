package dev.edujacksons.judge.api;

import java.util.List;
import java.util.UUID;

/**
 * Итог проверки решения: общий вердикт, число пройденных тестов и разбивка по тестам.
 * Публикуется судьёй в топик результатов, потребляется модулем submissions.
 */
public record JudgeResult(
        UUID submissionId,
        Verdict verdict,
        int passed,
        int total,
        List<TestResult> testResults
) {
    /** Результат одного теста: вердикт и метрики (могут быть {@code null}, если недоступны). */
    public record TestResult(UUID testCaseId, Verdict verdict, Integer timeMs, Integer memoryKb) {
    }
}
