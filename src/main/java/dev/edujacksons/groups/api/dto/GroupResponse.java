package dev.edujacksons.groups.api.dto;

import dev.edujacksons.groups.domain.Group;
import java.time.Instant;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        String title,
        Instant createdAt
) {
    public static GroupResponse from(Group group) {
        return new GroupResponse(group.getId(), group.getTitle(), group.getCreatedAt());
    }
}
