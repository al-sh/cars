package com.carsai.back.chat.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO для создания нового чата.
 *
 * Заголовок опционален — пользователь может создать чат без названия.
 * Title будет автоматически сгенерирован на основе первого сообщения (этап LLM).
 *
 * Пример запроса:
 *   POST /api/v1/chats
 *   {} или {"title": "Ищу кроссовер"}
 *
 * @RequestBody(required = false) в контроллере — тело запроса может отсутствовать.
 */
public record CreateChatRequest(
        // @Size без @NotBlank: title может быть null (тогда валидация пропускается),
        // но если передан — должен быть от 1 до 255 символов.
        @Size(min = 1, max = 255, message = "Заголовок от 1 до 255 символов")
        String title
) {}
