package com.carsai.back.message.dto;

import java.time.Instant;
import java.util.UUID;

import com.carsai.back.message.Message;
import com.carsai.back.message.MessageRole;

/**
 * DTO сообщения для отправки клиенту.
 *
 * Содержит все поля сообщения согласно контракту (types.md).
 * Entity Message содержит объект Chat (@ManyToOne) — DTO хранит только chatId.
 *
 * Поле role сериализуется в lower-case ("user", "assistant", "system")
 * благодаря @JsonValue в MessageRole enum.
 *
 * Пример JSON:
 * {
 *   "id": "550e8400-e29b-41d4-a716-446655440000",
 *   "chatId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
 *   "role": "user",
 *   "content": "Ищу кроссовер до 3 миллионов",
 *   "createdAt": "2025-01-15T10:30:00Z"
 * }
 */
public record MessageDto(
        UUID id,
        UUID chatId,
        MessageRole role,
        String content,
        Instant createdAt
) {

    /**
     * Фабричный метод: конвертирует Entity → DTO.
     *
     * message.getChat().getId() — обращение к LAZY-связи.
     * При загрузке списка сообщений нужно убедиться, что Chat уже в кеше
     * (или использовать @EntityGraph / JOIN FETCH в Repository).
     *
     * @param message Entity из БД
     * @return DTO для отправки клиенту
     */
    public static MessageDto from(Message message) {
        return new MessageDto(
                message.getId(),
                message.getChat().getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
