package com.carsai.back.common.exception;

import java.util.UUID;

/**
 * Исключение: сообщение не найдено или не принадлежит текущему пользователю.
 *
 * Аналог ChatNotFoundException, но для сущности Message.
 * Намеренно не различаем "не существует" и "чужое" — оба дают 404,
 * чтобы не раскрывать факт существования чужих сообщений.
 */
public class MessageNotFoundException extends RuntimeException {

    public MessageNotFoundException(UUID messageId) {
        super("Сообщение не найдено: " + messageId);
    }
}
