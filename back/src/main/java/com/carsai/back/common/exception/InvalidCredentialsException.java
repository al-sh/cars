package com.carsai.back.common.exception;

/**
 * Исключение: неверный email или пароль.
 *
 * Бросается в AuthService.login() при:
 * - email не найден в БД
 * - пароль не совпадает с хешем
 *
 * Сообщение намеренно общее ("Неверный email или пароль") —
 * не раскрываем, что именно неверно, чтобы не помогать перебору.
 *
 * Будет обработано в GlobalExceptionHandler (этап 8) → HTTP 401 Unauthorized.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Неверный email или пароль");
    }
}
