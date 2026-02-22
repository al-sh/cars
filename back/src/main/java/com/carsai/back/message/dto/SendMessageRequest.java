package com.carsai.back.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для отправки сообщения в чат.
 *
 * Пользователь всегда отправляет текст — role = USER проставляется автоматически.
 * Клиент не может указать role: только USER для входящих сообщений.
 * ASSISTANT-сообщения создаются сервером (ответ ИИ).
 *
 * Пример запроса:
 *   POST /api/v1/chats/{chatId}/messages
 *   {"content": "Ищу кроссовер до 3 миллионов, семья из 4 человек"}
 *
 * Ограничение 10 000 символов — разумный предел для сообщения в чате.
 * Защита от злоупотреблений: слишком длинный промпт нагружает LLM и дорого стоит.
 */
public record SendMessageRequest(
        @NotBlank(message = "Сообщение не может быть пустым")
        @Size(min = 1, max = 10_000, message = "Сообщение не более 10 000 символов")
        String content
) {}
