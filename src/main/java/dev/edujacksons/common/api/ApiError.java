package dev.edujacksons.common.api;

import java.time.Instant;
import java.util.Map;

/**
 * Единый формат тела ошибки API.
 *
 * @param status  HTTP-код
 * @param error   короткий машиночитаемый код ошибки
 * @param message человекочитаемое сообщение
 * @param fields  ошибки валидации по полям (может быть {@code null})
 * @param timestamp момент возникновения
 */
public record ApiError(
        int status,
        String error,
        String message,
        Map<String, String> fields,
        Instant timestamp
) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, null, Instant.now());
    }

    public static ApiError of(int status, String error, String message, Map<String, String> fields) {
        return new ApiError(status, error, message, fields, Instant.now());
    }
}
