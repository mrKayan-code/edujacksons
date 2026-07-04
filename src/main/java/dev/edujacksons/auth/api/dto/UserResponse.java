package dev.edujacksons.auth.api.dto;

import dev.edujacksons.auth.domain.Role;
import dev.edujacksons.auth.domain.User;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        Role role
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }
}
