package dev.edujacksons.submissions.api.dto;

import dev.edujacksons.common.domain.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSubmissionRequest(
        @NotNull Language language,
        @NotBlank String sourceCode
) {
}
