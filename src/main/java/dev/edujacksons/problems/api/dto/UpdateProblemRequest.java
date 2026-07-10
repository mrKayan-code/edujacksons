package dev.edujacksons.problems.api.dto;

import dev.edujacksons.common.domain.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateProblemRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String statement,
        @NotNull Language language,
        @NotNull @Positive Integer timeLimitMs,
        @NotNull @Positive Integer memoryLimitKb
) {
}
