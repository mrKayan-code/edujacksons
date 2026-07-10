package dev.edujacksons.groups.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Группа с участниками и привязанными курсами (для экрана группы). */
public record GroupDetailResponse(
        UUID id,
        String title,
        Instant createdAt,
        List<MemberResponse> members,
        List<AssignedCourseResponse> courses
) {
}
