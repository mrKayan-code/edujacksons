package dev.edujacksons.judge.service;

import dev.edujacksons.common.domain.Language;

/** Запуск одного прогона: язык, исходник, stdin и лимиты. Ожидаемый вывод сюда не входит. */
public record ExecutionRequest(
        Language language,
        String sourceCode,
        String stdin,
        int timeLimitMs,
        int memoryLimitKb
) {
}
