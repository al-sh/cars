package com.carsai.back.chat;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository для доступа к таблице chats через Spring Data JPA.
 *
 * Расширяет два интерфейса:
 * 1. JpaRepository<Chat, UUID> — базовые CRUD операции (save, findById, delete...)
 * 2. JpaSpecificationExecutor<Chat> — поддержка Specification-запросов (динамические фильтры).
 *    Нужен для ChatService.getUserChats() — там динамически строится WHERE-условие
 *    в зависимости от параметров (поиск по title, фильтрация deleted и т.д.).
 *    Аналогия: как Knex query builder, только на уровне Java-кода.
 *
 * Spring Data сам генерирует реализацию при запуске — никакого SQL писать не нужно
 * для стандартных запросов (derived query methods).
 */
@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID>, JpaSpecificationExecutor<Chat> {

    /**
     * Найти активный (не удалённый) чат по его ID и ID владельца.
     *
     * Spring Data разбирает имя метода:
     *   findBy + Id + And + UserId + And + DeletedFalse
     *   → SELECT * FROM chats WHERE id = ? AND user_id = ? AND deleted = false
     *
     * Используется для проверки прав доступа:
     * пользователь может видеть/изменять ТОЛЬКО свои чаты.
     * Если чат чужой или удалён — возвращает Optional.empty() → выбрасываем ChatNotFoundException.
     *
     * @param id     UUID чата
     * @param userId UUID владельца чата
     * @return Optional с чатом, или пустой если не найден / чужой / удалён
     */
    Optional<Chat> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    /**
     * Подсчитать количество активных сообщений в чате.
     *
     * @Query — кастомный JPQL-запрос. JPQL работает с Entity-классами, а не с таблицами:
     *   "Message m" вместо "messages m"
     *   "m.chat.id" вместо "m.chat_id"
     *
     * Зачем: ChatDto включает поле messageCount согласно контракту API.
     * Chat entity не хранит счётчик — считаем на лету через COUNT.
     *
     * В Prisma аналог: _count: { select: { messages: true } }
     * В SQL: SELECT COUNT(*) FROM messages WHERE chat_id = ?
     *
     * @param chatId UUID чата
     * @return количество сообщений в чате
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.chat.id = :chatId")
    int countMessagesByChatId(@Param("chatId") UUID chatId);
}
