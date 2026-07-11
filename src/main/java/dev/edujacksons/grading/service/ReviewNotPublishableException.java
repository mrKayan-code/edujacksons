package dev.edujacksons.grading.service;

import dev.edujacksons.common.api.ApiException;
import org.springframework.http.HttpStatus;

/** Публикация запрещена, так как нет ни оценки, ни фидбэка. → HTTP 400. */
public class ReviewNotPublishableException extends ApiException {
    public ReviewNotPublishableException() {
        super(HttpStatus.BAD_REQUEST, "review_not_publishable", "Нельзя опубликовать пустую проверку: укажите оценку или комментарий");
    }
}
