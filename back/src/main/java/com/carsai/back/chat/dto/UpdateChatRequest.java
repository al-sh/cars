package com.carsai.back.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для обновления заголовка чата.
 *
 * В отличие от CreateChatRequest, здесь title обязателен:
 * PATCH /api/v1/chats/{id} используется только для переименования чата.
 *
 * Пример запроса:
 *   PATCH /api/v1/chats/550e8400-...
 *   {"title": "Новое название чата"}
 */
public record UpdateChatRequest(
        // @NotBlank — не null, не пустая строка, не только пробелы.
        // При переименовании пользователь обязан указать непустой заголовок.
        @NotBlank(message = "Заголовок обязателен")
        @Size(min = 1, max = 255, message = "Заголовок от 1 до 255 символов")
        String title
) {}
