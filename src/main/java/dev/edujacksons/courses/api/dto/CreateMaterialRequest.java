package dev.edujacksons.courses.api.dto;

import dev.edujacksons.courses.domain.MaterialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Создание материала. {@code orderIndex} необязателен: если не задан — материал встаёт в конец.
 * {@code body} — markdown (слайды через {@code ---}).
 */
public record CreateMaterialRequest(
        @NotNull MaterialType type,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String body,
        Integer orderIndex
) {
}
