package dev.edujacksons.grading.service;

import dev.edujacksons.common.api.ApiException;
import org.springframework.http.HttpStatus;

/** Проверка не найдена (или черновик, запрошенный учеником). → HTTP 404. */
public class ReviewNotFoundException extends ApiException {
    public ReviewNotFoundException() {
        super(HttpStatus.NOT_FOUND, "review_not_found", "Проверка не найдена или ещё не опубликована");
    }
}
