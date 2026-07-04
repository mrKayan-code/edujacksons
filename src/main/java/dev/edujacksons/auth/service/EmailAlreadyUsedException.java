package dev.edujacksons.auth.service;

import dev.edujacksons.common.api.ApiException;
import org.springframework.http.HttpStatus;

/** Регистрация с уже занятым email. */
public class EmailAlreadyUsedException extends ApiException {

    public EmailAlreadyUsedException(String email) {
        super(HttpStatus.CONFLICT, "email_already_used", "Email уже зарегистрирован: " + email);
    }
}
