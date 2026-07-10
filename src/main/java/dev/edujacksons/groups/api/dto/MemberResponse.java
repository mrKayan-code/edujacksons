package dev.edujacksons.groups.api.dto;

import java.time.Instant;
import java.util.UUID;

/** Участник группы, обогащённый данными из auth (email, имя) через UserDirectory. */
public record MemberResponse(
        UUID studentId,
        String email,
        String displayName,
        Instant joinedAt
) {
}
