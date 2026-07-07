package dev.edujacksons.groups.service;

import dev.edujacksons.common.api.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Группа не найдена. → HTTP 404. */
public class GroupNotFoundException extends ApiException {

    public GroupNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "group_not_found", "Группа не найдена: " + id);
    }
}
