package dev.edujacksons.groups.api.dto;

import dev.edujacksons.groups.domain.GroupCourse;
import java.time.Instant;
import java.util.UUID;

/** Курс, привязанный к группе. */
public record AssignedCourseResponse(
        UUID courseId,
        Instant assignedAt
) {
    public static AssignedCourseResponse from(GroupCourse groupCourse) {
        return new AssignedCourseResponse(groupCourse.getCourseId(), groupCourse.getCreatedAt());
    }
}
