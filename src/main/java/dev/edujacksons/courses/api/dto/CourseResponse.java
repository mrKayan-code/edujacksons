package dev.edujacksons.courses.api.dto;

import dev.edujacksons.courses.domain.Course;
import java.time.Instant;
import java.util.UUID;

public record CourseResponse(
        UUID id,
        UUID ownerId,
        String title,
        String description,
        Instant createdAt
) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(), course.getOwnerId(), course.getTitle(),
                course.getDescription(), course.getCreatedAt());
    }
}
