package com.carsai.back.common.exception;

import java.util.UUID;

/**
 * Чат не найден или не принадлежит текущему пользователю.
 *
 * Намеренно не раскрываем разницу между "не существует" и "чужой" —
 * оба случая возвращают 404. Это предотвращает enumeration-атаку:
 * злоумышленник не сможет перебором выяснить, какие UUID существуют.
 *
 * Перехватывается в GlobalExceptionHandler → 404 Not Found.
 *
 * RuntimeException (unchecked) — не нужно объявлять в throws.
 * Spring автоматически поймает через @RestControllerAdvice.
 */
public class ChatNotFoundException extends RuntimeException {

    public ChatNotFoundException(UUID chatId) {
        super("Чат не найден: " + chatId);
    }
}
