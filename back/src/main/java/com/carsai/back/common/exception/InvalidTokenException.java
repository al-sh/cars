package com.carsai.back.common.exception;

/**
 * Исключение: невалидный или истёкший JWT-токен.
 *
 * Будет использоваться в JwtService (этап 6) при проверке токена.
 * Создаётся сейчас, чтобы exception-пакет был сразу полным.
 *
 * Будет обработано в GlobalExceptionHandler (этап 8) → HTTP 401 Unauthorized.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
