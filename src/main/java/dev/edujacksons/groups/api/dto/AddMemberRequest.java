package dev.edujacksons.groups.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Добавление ученика в группу по email уже зарегистрированного пользователя. */
public record AddMemberRequest(
        @NotBlank @Email String email
) {
}
