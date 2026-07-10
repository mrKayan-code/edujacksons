package dev.edujacksons.problems.service;

import java.util.UUID;

/**
 * View-запись одного теста для прогона (вход + ожидаемый вывод). Отдаётся модулю submissions
 * при сборке заявки на проверку — без утечки JPA-сущности {@code TestCase}.
 */
public record TestCaseData(UUID testCaseId, String input, String expectedOutput) {
}
