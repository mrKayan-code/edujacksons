package dev.edujacksons.groups.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Привязка курса к группе. */
public record AssignCourseRequest(
        @NotNull UUID courseId
) {
}
