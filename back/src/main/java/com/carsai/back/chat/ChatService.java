package com.carsai.back.chat;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsai.back.chat.dto.ChatDto;
import com.carsai.back.common.dto.PagedResponse;
import com.carsai.back.common.exception.ChatNotFoundException;
import com.carsai.back.user.User;
import com.carsai.back.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Сервис чатов — бизнес-логика CRUD операций с чатами.
 *
 * Слоистая архитектура:
 *   ChatController (HTTP) → ChatService (логика) → ChatRepository (БД)
 *
 * Ключевой принцип: каждый метод сначала проверяет,
 * что пользователь имеет права на операцию с чатом.
 * Пользователь не может читать/изменять чужие чаты.
 *
 * @Service — Spring создаёт singleton и регистрирует в DI-контейнере.
 * @RequiredArgsConstructor — Lombok генерирует конструктор из final-полей.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    /**
     * Список чатов пользователя с пагинацией и опциональным поиском по заголовку.
     *
     * Specification — динамический построитель WHERE-условий.
     * В отличие от фиксированных derived query методов (findByUserIdAndDeletedFalse),
     * Specification позволяет добавлять условия программно в зависимости от параметров.
     *
     * Аналогия:
     * - Express/Knex: knex('chats').where('user_id', userId).where('deleted', false)
     *   .modify(builder => { if (search) builder.whereLike('title', `%${search}%`) })
     * - Angular: computed() который фильтрует массив чатов по разным условиям
     *
     * ИЗВЕСТНОЕ ОГРАНИЧЕНИЕ (N+1 запросов):
     * Для каждого чата выполняется дополнительный запрос countMessagesByChatId.
     * При 20 чатах на странице = 21 SQL-запрос (1 + 20).
     * Приемлемо для MVP — при необходимости оптимизировать через @Query с LEFT JOIN COUNT.
     *
     * @param userId  UUID пользователя из JWT
     * @param page    номер страницы (с 1)
     * @param perPage элементов на странице
     * @param search  поисковый запрос по заголовку (nullable)
     * @return PagedResponse<ChatDto> с метаданными пагинации
     */
    public PagedResponse<ChatDto> getUserChats(UUID userId, int page, int perPage, String search) {
        // Базовое условие: только чаты этого пользователя и только активные (не удалённые).
        // Specification.where() — начало цепочки условий.
        // (root, query, cb) — лямбда: root = Chat entity, cb = CriteriaBuilder (строитель условий).
        Specification<Chat> spec = Specification
                .<Chat>where((root, query, cb) ->
                        cb.equal(root.get("user").get("id"), userId))
                .and((root, query, cb) ->
                        cb.isFalse(root.get("deleted")));

        // Динамически добавляем поиск по title если передан.
        // cb.lower() + toLowerCase() — регистронезависимый поиск.
        // cb.like() → LIKE '%search%' — ищет подстроку в title.
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("title")), pattern));
        }

        // PageRequest.of() — настройки пагинации для Spring Data:
        // - page - 1: Spring считает страницы с 0, наш API — с 1 (вычитаем 1)
        // - perPage: сколько чатов на странице
        // - Sort.by("updatedAt").descending(): ORDER BY updated_at DESC — самый активный вверху
        Page<Chat> chats = chatRepository.findAll(spec,
                PageRequest.of(page - 1, perPage, Sort.by("updatedAt").descending()));

        // Для каждого чата запрашиваем количество сообщений (N+1, приемлемо для MVP).
        return PagedResponse.from(chats, chat ->
                ChatDto.from(chat, chatRepository.countMessagesByChatId(chat.getId())));
    }

    /**
     * Получить один чат по ID (с проверкой владельца).
     *
     * Если чат не существует, удалён или принадлежит другому пользователю —
     * выбрасываем ChatNotFoundException → 404 (не раскрываем причину).
     *
     * @param userId UUID пользователя из JWT
     * @param chatId UUID чата
     * @return ChatDto
     * @throws ChatNotFoundException если чат не найден или не принадлежит пользователю
     */
    public ChatDto getChatById(UUID userId, UUID chatId) {
        Chat chat = chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId)
                .orElseThrow(() -> new ChatNotFoundException(chatId));
        return ChatDto.from(chat, chatRepository.countMessagesByChatId(chatId));
    }

    /**
     * Создать новый чат.
     *
     * @Transactional — если save() упадёт, транзакция откатится.
     * Analogia Express: await db.transaction(async trx => { ... })
     *
     * Алгоритм:
     * 1. Получить User-reference по userId (нужен для связи @ManyToOne в Chat)
     * 2. Создать Chat entity через Builder
     * 3. Сохранить в БД
     * 4. Вернуть DTO
     *
     * @param userId UUID пользователя из JWT
     * @param title  заголовок чата (nullable)
     * @return ChatDto созданного чата
     */
    @Transactional
    public ChatDto createChat(UUID userId, String title) {
        // getReferenceById() — получаем "прокси" объекта User без загрузки из БД.
        // Нам не нужны данные пользователя — только его ID для связи @ManyToOne.
        // getReferenceById vs findById:
        //   findById → SELECT * FROM users WHERE id = ? (загружает все поля)
        //   getReferenceById → создаёт прокси, не делает запрос к БД
        // Экономит один SQL-запрос при создании чата.
        User userRef = userRepository.getReferenceById(userId);

        Chat chat = Chat.builder()
                .user(userRef)
                .title(title != null ? title.trim() : null)
                .deleted(false)
                .build();
        // @PrePersist в Chat установит createdAt и updatedAt автоматически.

        Chat saved = chatRepository.save(chat);
        // Новый чат — 0 сообщений. Не делаем лишний COUNT запрос.
        return ChatDto.from(saved, 0);
    }

    /**
     * Обновить заголовок чата.
     *
     * Сначала проверяем права доступа — findByIdAndUserIdAndDeletedFalse.
     * Если чат найден (и наш), обновляем title и сохраняем.
     * @PreUpdate в Chat установит updatedAt = now() автоматически.
     *
     * @param userId UUID пользователя из JWT
     * @param chatId UUID чата для обновления
     * @param title  новый заголовок (обязателен, проверяется @NotBlank в контроллере)
     * @return ChatDto с обновлёнными данными
     * @throws ChatNotFoundException если чат не найден или не принадлежит пользователю
     */
    @Transactional
    public ChatDto updateChat(UUID userId, UUID chatId, String title) {
        Chat chat = chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId)
                .orElseThrow(() -> new ChatNotFoundException(chatId));

        chat.setTitle(title.trim());
        // chatRepository.save() → UPDATE chats SET title = ?, updated_at = ? WHERE id = ?
        // @PreUpdate вызовется автоматически перед UPDATE и установит updatedAt.
        Chat saved = chatRepository.save(chat);

        return ChatDto.from(saved, chatRepository.countMessagesByChatId(chatId));
    }

    /**
     * Удалить чат (soft delete — ставим deleted = true).
     *
     * Физического удаления нет — данные сохраняются в БД, но скрыты от пользователя.
     * Каскадного удаления сообщений не делаем — они тоже остаются (soft delete on message side).
     * В будущем: отдельная задача очистки устаревших данных (housekeeping job).
     *
     * @param userId UUID пользователя из JWT
     * @param chatId UUID чата для удаления
     * @throws ChatNotFoundException если чат не найден или не принадлежит пользователю
     */
    @Transactional
    public void deleteChat(UUID userId, UUID chatId) {
        Chat chat = chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId)
                .orElseThrow(() -> new ChatNotFoundException(chatId));

        // Soft delete: не DELETE FROM chats WHERE id = ?,
        // а UPDATE chats SET deleted = true WHERE id = ?
        chat.setDeleted(true);
        chatRepository.save(chat);
    }
}
