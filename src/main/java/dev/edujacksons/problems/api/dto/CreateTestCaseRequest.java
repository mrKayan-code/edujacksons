package dev.edujacksons.problems.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Тест задачи. {@code input}/{@code expectedOutput} могут быть пустыми строками (пустой stdin),
 * поэтому {@code @NotNull}, а не {@code @NotBlank}. {@code sample}/{@code orderIndex} — необязательны.
 */
public record CreateTestCaseRequest(
        @NotNull String input,
        @NotNull String expectedOutput,
        Boolean sample,
        Integer orderIndex
) {
}
