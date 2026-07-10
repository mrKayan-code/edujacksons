package dev.edujacksons.judge.api;

import dev.edujacksons.common.domain.Language;
import java.util.List;
import java.util.UUID;

/**
 * Заявка на проверку решения — <b>самодостаточна</b>: несёт код, язык, лимиты и полный набор
 * тестов. Судья не заглядывает в чужие таблицы, только исполняет её. Это и есть будущая граница
 * микросервиса judge: при выносе меняется лишь транспорт (Kafka → сеть), а не логика (ADR 0006).
 */
public record JudgeRequest(
        UUID submissionId,
        Language language,
        String sourceCode,
        int timeLimitMs,
        int memoryLimitKb,
        List<JudgeTestCase> tests
) {
    /** Один тест заявки: stdin и ожидаемый stdout. */
    public record JudgeTestCase(UUID testCaseId, String input, String expectedOutput) {
    }
}
