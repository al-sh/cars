package com.carsai.back.chat.dto;

import java.time.Instant;
import java.util.UUID;

import com.carsai.back.chat.Chat;

/**
 * DTO чата для отправки клиенту.
 *
 * Содержит публичные данные чата согласно контракту API (types.md).
 * Entity Chat содержит объект User (связь @ManyToOne) —
 * DTO хранит только userId (UUID), чтобы не тянуть лишние данные.
 *
 * Дополнительно включает messageCount — количество сообщений в чате.
 * Chat entity не хранит счётчик, поэтому factory method принимает его отдельно.
 *
 * Пример JSON:
 * {
 *   "id": "550e8400-e29b-41d4-a716-446655440000",
 *   "userId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
 *   "title": "Седан до 2 млн",
 *   "createdAt": "2025-01-15T10:30:00Z",
 *   "updatedAt": "2025-01-15T11:45:00Z",
 *   "messageCount": 12
 * }
 */
public record ChatDto(
        UUID id,
        UUID userId,
        String title,        // nullable — заголовок устанавливается после первого сообщения
        Instant createdAt,
        Instant updatedAt,
        int messageCount     // считается отдельным запросом в ChatRepository
) {

    /**
     * Фабричный метод: конвертирует Entity + счётчик сообщений → DTO.
     *
     * Принимает messageCount отдельно, т.к. Chat entity не хранит этот счётчик.
     * В Service: ChatDto.from(chat, chatRepository.countMessagesByChatId(chat.getId()))
     *
     * chat.getUser().getId() — обращение к LAZY-связи.
     * Hibernate загрузит User только если он ещё не в кеше первого уровня.
     * Чтобы избежать N+1 проблемы при загрузке списка чатов,
     * в Specification-запросе (ChatService) нужно делать JOIN FETCH.
     *
     * @param chat         Entity из БД
     * @param messageCount количество сообщений (из countMessagesByChatId)
     * @return DTO для отправки клиенту
     */
    public static ChatDto from(Chat chat, int messageCount) {
        return new ChatDto(
                chat.getId(),
                chat.getUser().getId(),
                chat.getTitle(),
                chat.getCreatedAt(),
                chat.getUpdatedAt(),
                messageCount
        );
    }
}
