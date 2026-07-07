package dev.edujacksons.courses.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Полная замена редактируемых полей курса. */
public record UpdateCourseRequest(
        @NotBlank @Size(max = 200) String title,
        String description
) {
}
