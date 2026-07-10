package dev.edujacksons.courses.api.dto;

import dev.edujacksons.courses.domain.MaterialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Полная замена редактируемых полей материала. */
public record UpdateMaterialRequest(
        @NotNull MaterialType type,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String body,
        @NotNull Integer orderIndex
) {
}
