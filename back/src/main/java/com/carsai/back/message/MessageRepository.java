package com.carsai.back.message;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository для доступа к таблице messages через Spring Data JPA.
 *
 * Используется в MessageService для cursor-based пагинации сообщений.
 *
 * Cursor-пагинация (vs offset-пагинация):
 *   Offset: SELECT ... LIMIT 20 OFFSET 40  — нестабильно при новых сообщениях
 *   Cursor: SELECT ... WHERE created_at < ? ORDER BY created_at DESC LIMIT 20
 *           — стабильно: новое сообщение не сдвигает позицию в истории
 *
 * В Prisma аналог cursor-пагинации:
 *   prisma.message.findMany({ where: { chatId }, cursor: { id: lastId }, take: 20 })
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Загрузить сообщения чата (последние N штук).
     *
     * Derived query: findBy + ChatId → WHERE chat_id = ?
     * Pageable задаёт LIMIT и ORDER BY:
     *   PageRequest.of(0, limit, Sort.by("createdAt").descending())
     *   → ORDER BY created_at DESC LIMIT ?
     *
     * Используется при первой загрузке чата (без курсора).
     *
     * @param chatId   UUID чата
     * @param pageable настройки пагинации (limit + sort)
     * @return список сообщений в порядке убывания created_at
     */
    List<Message> findByChatId(UUID chatId, Pageable pageable);

    /**
     * Загрузить сообщения чата, созданные ДО указанного момента времени (cursor).
     *
     * Derived query: findBy + ChatId + And + CreatedAtBefore
     *   → WHERE chat_id = ? AND created_at < ?
     *
     * Используется при подгрузке истории (infinite scroll вверх):
     * клиент отправляет ID последнего видимого сообщения,
     * сервис находит его created_at и делает этот запрос.
     *
     * Почему before по времени, а не по ID?
     * ID (UUID) не упорядочен хронологически — нельзя сравнивать "<" или ">".
     * created_at — числовая временная метка, поддерживает сравнение.
     *
     * @param chatId    UUID чата
     * @param createdAt граница времени (exclusive — сообщения строго ДО этого момента)
     * @param pageable  настройки пагинации (limit + sort)
     * @return список сообщений до указанного момента
     */
    List<Message> findByChatIdAndCreatedAtBefore(UUID chatId, Instant createdAt, Pageable pageable);
}
