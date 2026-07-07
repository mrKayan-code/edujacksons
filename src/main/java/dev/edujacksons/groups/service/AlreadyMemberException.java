package dev.edujacksons.groups.service;

import dev.edujacksons.common.api.ApiException;
import org.springframework.http.HttpStatus;

/** Ученик уже состоит в группе. → HTTP 409. */
public class AlreadyMemberException extends ApiException {

    public AlreadyMemberException() {
        super(HttpStatus.CONFLICT, "already_member", "Ученик уже состоит в группе");
    }
}
