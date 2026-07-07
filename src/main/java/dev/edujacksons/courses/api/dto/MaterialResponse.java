package dev.edujacksons.courses.api.dto;

import dev.edujacksons.courses.domain.Material;
import dev.edujacksons.courses.domain.MaterialType;
import java.time.Instant;
import java.util.UUID;

public record MaterialResponse(
        UUID id,
        UUID courseId,
        MaterialType type,
        String title,
        String body,
        int orderIndex,
        Instant createdAt
) {
    public static MaterialResponse from(Material material) {
        return new MaterialResponse(
                material.getId(), material.getCourseId(), material.getType(), material.getTitle(),
                material.getBody(), material.getOrderIndex(), material.getCreatedAt());
    }
}
