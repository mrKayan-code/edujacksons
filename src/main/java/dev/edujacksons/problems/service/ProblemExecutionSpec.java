package dev.edujacksons.problems.service;

import dev.edujacksons.common.domain.Language;
import java.util.List;
import java.util.UUID;

/**
 * Всё, что нужно для проверки решения задачи: язык, лимиты и полный набор тестов
 * (включая скрытые). View-запись межмодульной границы — модуль submissions кладёт её
 * в самодостаточную заявку на проверку (JudgeRequest), не заглядывая в таблицы problems.
 */
public record ProblemExecutionSpec(
        UUID problemId,
        Language language,
        int timeLimitMs,
        int memoryLimitKb,
        List<TestCaseData> tests
) {
}
