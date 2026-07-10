package dev.edujacksons.courses.api.dto;

import dev.edujacksons.courses.domain.Course;
import dev.edujacksons.courses.domain.Material;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Курс вместе с его материалами (для экрана курса). */
public record CourseDetailResponse(
        UUID id,
        UUID ownerId,
        String title,
        String description,
        Instant createdAt,
        List<MaterialResponse> materials
) {
    public static CourseDetailResponse from(Course course, List<Material> materials) {
        return new CourseDetailResponse(
                course.getId(), course.getOwnerId(), course.getTitle(), course.getDescription(),
                course.getCreatedAt(), materials.stream().map(MaterialResponse::from).toList());
    }
}
