package dev.edujacksons.common.api;

import org.springframework.http.HttpStatus;

/** Базовое доменное исключение с HTTP-статусом и машиночитаемым кодом. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
