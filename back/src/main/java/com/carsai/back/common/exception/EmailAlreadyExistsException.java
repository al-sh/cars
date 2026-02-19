package com.carsai.back.common.exception;

/**
 * Исключение: email уже зарегистрирован.
 *
 * Бросается в AuthService.register() при попытке регистрации с email,
 * который уже есть в таблице users.
 *
 * Будет обработано в GlobalExceptionHandler (этап 8) → HTTP 409 Conflict.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email уже зарегистрирован: " + email);
    }
}
