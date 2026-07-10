package dev.edujacksons.groups.service;

import java.time.Instant;
import java.util.UUID;

/** Участник группы, обогащённый данными из auth. Сервис-слойное представление (контроллер маппит в DTO). */
public record MemberView(UUID studentId, String email, String displayName, Instant joinedAt) {
}
